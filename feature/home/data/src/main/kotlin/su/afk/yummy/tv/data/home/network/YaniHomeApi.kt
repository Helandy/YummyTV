package su.afk.yummy.tv.data.home.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.home.dto.YaniFeedDto

class YaniHomeApi(
    private val client: HttpClient,
) {
    suspend fun getFeed(): YaniFeedDto {
        val body = client.get("$YANI_BASE_URL/feed").bodyAsText()
        logFeedResponse(body = body)
        return FEED_JSON.decodeFromString(body)
    }

    private fun logFeedResponse(body: String) {
        Log.i(
            TAG,
            "Feed response chars=${body.length} " +
                    body.feedKeysForLog(),
        )
    }

    private fun String.feedKeysForLog(): String =
        runCatching {
            val root = FEED_JSON.parseToJsonElement(this).jsonObject
            val response = root["response"] as? JsonObject
            val lastWatches = response?.get("last_watches") as? JsonArray
            val rootKeys = root.keys.sorted().joinToString(prefix = "[", postfix = "]")
            val responseKeys = response?.keys
                ?.sorted()
                ?.joinToString(prefix = "[", postfix = "]")
                ?: "[]"
            "rootKeys=$rootKeys responseKeys=$responseKeys lastWatches=${lastWatches?.size ?: 0}"
        }.getOrElse { error ->
            "keysError=${error::class.java.simpleName}"
        }

    private companion object {
        const val TAG = "YaniHomeFeed"
        val FEED_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
