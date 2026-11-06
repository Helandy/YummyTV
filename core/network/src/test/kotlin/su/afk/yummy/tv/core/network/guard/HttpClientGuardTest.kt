package su.afk.yummy.tv.core.network.guard

import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.io.File

/**
 * Сторож: сетевые клиенты создаются только в `core/network`. Новый `OkHttpClient.Builder()`, Ktor `HttpClient(...)`
 * или `HttpURLConnection` в другом модуле получил бы свой пул, DNS и таймауты в обход приложения. Клиент берут через DI
 * (базовый `OkHttpClient`, `@StreamingOkHttp`, `@DownloadOkHttp`, `@AllohaOkHttp`, Ktor-квалификаторы) и собирают
 * через `okHttpKtorClient`. Законные исключения перечислены в [ALLOWED] с причиной.
 */
class HttpClientGuardTest : BaseUnitTest() {

    @Test
    fun `network clients are created only in core network`() {
        val violations = sourceFiles()
            .filterNot { it.invariantPath().contains("/core/network/") }
            .flatMap { file -> violationsIn(file) }
            .filterNot { violation -> ALLOWED.any { violation.path.endsWith(it.pathSuffix) && violation.rule == it.rule } }
            .toList()

        assertTrue(
            "Клиенты создаются вне core/network (используйте DI и okHttpKtorClient):\n" +
                violations.joinToString("\n") { "  ${it.path}: ${it.rule}" },
            violations.isEmpty(),
        )
    }

    @Test
    fun `the scan sees the project sources`() {
        val files = sourceFiles().toList()

        assertTrue("Сторож не нашёл исходники (${files.size})", files.size > 200)
    }

    @Test
    fun `rules catch every way to create a client`() {
        val samples = mapOf(
            "OkHttpClient.Builder().build()" to "OkHttpClient.Builder()",
            "val c = OkHttpClient()" to "OkHttpClient()",
            "HttpClient(OkHttp) { }" to "Ktor HttpClient(...)",
            "HttpClient(CIO) { }" to "Ktor HttpClient(...)",
            "url.openConnection()" to "openConnection()",
            "val c: HttpURLConnection" to "HttpURLConnection",
            "DefaultHttpDataSource.Factory()" to "DefaultHttpDataSource",
            "URL(x).openStream()" to "URL.openStream()",
        )

        samples.forEach { (code, rule) ->
            assertTrue("Не поймано: $code", RULES.any { it.first == rule && it.second.containsMatchIn(code) })
        }
        assertTrue("PlayerHttpClient — не клиент", RULES.none { it.second.containsMatchIn("class KtorPlayerHttpClient(") })
    }

    private class Violation(val path: String, val rule: String)

    private class Allowed(val pathSuffix: String, val rule: String, val reason: String)

    private fun violationsIn(file: File): List<Violation> {
        val code = file.readText().withoutComments()
        return RULES.filter { (_, regex) -> regex.containsMatchIn(code) }
            .map { (rule, _) -> Violation(file.invariantPath(), rule) }
    }

    private fun sourceFiles(): Sequence<File> = repositoryRoot().walkTopDown()
        .onEnter { it.name !in SKIPPED_DIRS }
        .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
        .filter { it.invariantPath().contains("/src/main/") }

    private fun repositoryRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) dir = dir.parentFile
        return requireNotNull(dir) { "Не найден корень репозитория (settings.gradle.kts)" }
    }

    private fun File.invariantPath(): String = path.replace(File.separatorChar, '/')

    private fun String.withoutComments(): String =
        replace(BLOCK_COMMENT, "").replace(LINE_COMMENT, "")

    private companion object {
        val SKIPPED_DIRS = setOf("build", ".git", ".gradle", ".idea", "node_modules")
        val BLOCK_COMMENT = Regex("/\\*.*?\\*/", RegexOption.DOT_MATCHES_ALL)
        val LINE_COMMENT = Regex("(?m)(?<!:)//.*$")

        val RULES: List<Pair<String, Regex>> = listOf(
            "OkHttpClient.Builder()" to Regex("""OkHttpClient\.Builder\(\)"""),
            "OkHttpClient()" to Regex("""(?<![A-Za-z.])OkHttpClient\(\)"""),
            "Ktor HttpClient(...)" to Regex("""(?<![A-Za-z])HttpClient\("""),
            "HttpURLConnection" to Regex("""HttpURLConnection"""),
            "openConnection()" to Regex("""\.openConnection\("""),
            "DefaultHttpDataSource" to Regex("""DefaultHttpDataSource"""),
            "URL.openStream()" to Regex("""\.openStream\(\)"""),
        )

        val ALLOWED: List<Allowed> = listOf(
            Allowed(
                pathSuffix = "feature/account/data/src/main/kotlin/su/afk/yummy/tv/data/account/di/AccountDataModule.kt",
                rule = "Ktor HttpClient(...)",
                reason = "Передача сессии по LAN: OkHttp не пускает cleartext на 192.168.x.x, поэтому CIO на сырых сокетах",
            ),
        )
    }
}
