package su.afk.yummy.tv.buildlogic

import groovy.json.JsonSlurper
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.util.Locale

/**
 * Читает `*-benchmarkData.json` Macrobenchmark и печатает по каждому устройству таблицу:
 * метрика — без профиля (`None`) — с профилем (`BaselineProfile`) — разница.
 * Режим берётся из имени параметризованного теста: `startup[None]`, `startup[BaselineProfile]`.
 */
@DisableCachingByDefault(because = "Отчёт по результатам прогона на устройствах")
abstract class BenchmarkReportTask : DefaultTask() {

    init {
        outputs.upToDateWhen { false }
    }

    // @Internal, а не @InputDirectory: до первого прогона каталога нет, и валидация уронила бы задачу
    @get:Internal
    abstract val resultsDirectory: DirectoryProperty

    @get:OutputFile
    abstract val reportFile: RegularFileProperty

    @TaskAction
    fun report() {
        val files = resultsDirectory.get().asFile.walkTopDown()
            .filter { it.isFile && it.name.endsWith("benchmarkData.json") }
            .filter { file -> file.path.contains("benchmark", ignoreCase = true) }
            .toList()
        val report = if (files.isEmpty()) {
            "Результатов бенчмарков нет в ${resultsDirectory.get().asFile}. Сначала: ./gradlew runProfileBenchmarks"
        } else {
            files.sortedBy { it.path }.joinToString("\n\n") { deviceReport(it) }
        }
        reportFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText(report + "\n")
        }
        logger.lifecycle("\n$report\n\nОтчёт: ${reportFile.get().asFile}")
    }

    @Suppress("UNCHECKED_CAST")
    private fun deviceReport(file: File): String {
        val json = JsonSlurper().parse(file) as Map<String, Any?>
        val build = (json["context"] as? Map<String, Any?>)?.get("build") as? Map<String, Any?>
        val device = listOfNotNull(build?.get("model"), build?.get("device"))
            .joinToString(" / ").ifEmpty { file.parentFile.name }
        val benchmarks = json["benchmarks"] as? List<Map<String, Any?>> ?: emptyList()

        // тест без режима → метрика → режим → значение
        val rows = sortedMapOf<String, MutableMap<String, Double>>()
        benchmarks.forEach { benchmark ->
            val fullName = benchmark["name"] as? String ?: return@forEach
            val mode = MODE_REGEX.find(fullName)?.groupValues?.get(1) ?: return@forEach
            val test = "${(benchmark["className"] as? String)?.substringAfterLast('.')}." +
                fullName.substringBefore('[')
            metricValues(benchmark).forEach { (metric, value) ->
                rows.getOrPut("$test — $metric") { mutableMapOf() }[mode] = value
            }
        }

        val table = rows.entries.joinToString("\n") { (name, byMode) ->
            val none = byMode["None"]
            val profile = byMode["BaselineProfile"]
            val delta = if (none != null && profile != null && none != 0.0) {
                format((profile - none) / none * 100, "%+.1f %%")
            } else {
                "—"
            }
            "| $name | ${none.fmt()} | ${profile.fmt()} | $delta |"
        }
        // без trimMargin: он срезал бы ведущий «|» у строк таблицы
        return listOf(
            "### $device",
            "",
            "| Метрика (медиана / перцентиль; frameCount — штуки, остальное — мс) | Без профиля | С профилем | Разница |",
            "|---|---:|---:|---:|",
            table,
        ).joinToString("\n")
    }

    @Suppress("UNCHECKED_CAST")
    private fun metricValues(benchmark: Map<String, Any?>): List<Pair<String, Double>> {
        val metrics = benchmark["metrics"] as? Map<String, Map<String, Any?>> ?: emptyMap()
        val sampled = benchmark["sampledMetrics"] as? Map<String, Map<String, Any?>> ?: emptyMap()
        val single = metrics.mapNotNull { (name, stats) ->
            (stats["median"] as? Number)?.let { "$name (медиана)" to it.toDouble() }
        }
        val percentiles = sampled.flatMap { (name, stats) ->
            listOf("P50", "P90", "P99").mapNotNull { p ->
                (stats[p] as? Number)?.let { "$name $p" to it.toDouble() }
            }
        }
        return single + percentiles
    }

    private fun Double?.fmt(): String = this?.let { format(it, "%.1f") } ?: "—"

    private fun format(value: Double, pattern: String): String = String.format(Locale.ROOT, pattern, value)

    private companion object {
        val MODE_REGEX = Regex("""\[(\w+)]$""")
    }
}
