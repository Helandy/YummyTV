package su.afk.yummy.tv.core.network.statics

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.get
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppVersionHeaderPluginTest {

    private fun client(version: String, seen: MutableList<Pair<String, String?>>) = HttpClient(
        MockEngine { request ->
            seen += request.url.host to request.headers[APP_VERSION_HEADER]
            respondOk()
        },
    ) {
        install(AppVersionHeaderPlugin) {
            host = { "static.example" }
            this.version = version
        }
    }

    @Test
    fun `version goes only to our own static host`() = runBlocking {
        val seen = mutableListOf<Pair<String, String?>>()
        val client = client("1.28.0", seen)

        client.get("https://static.example/img/still/a.avif")
        client.get("https://STATIC.example/posters/mega/1.avif")
        client.get("https://image.tmdb.org/t/p/w780/a.jpg")

        assertEquals(
            listOf("static.example" to "1.28.0", "STATIC.example" to "1.28.0", "image.tmdb.org" to null),
            seen,
        )
    }

    @Test
    fun `blank version or unknown host adds nothing`() = runBlocking {
        val seen = mutableListOf<Pair<String, String?>>()

        client("", seen).get("https://static.example/a")

        assertNull(seen.single().second)
    }
}
