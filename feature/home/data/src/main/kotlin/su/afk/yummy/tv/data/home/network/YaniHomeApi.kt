package su.afk.yummy.tv.data.home.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import su.afk.yummy.tv.core.network.YANI_BASE_URL
import su.afk.yummy.tv.data.home.BuildConfig
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
        if (!BuildConfig.DEBUG) return
        val root = runCatching { FEED_JSON.parseToJsonElement(body).jsonObject }
        Log.d(
            TAG,
            "Feed response chars=${body.length} " +
                    root.fold(
                        onSuccess = { it.feedKeysForLog() },
                        onFailure = { "keysError=${it::class.java.simpleName}" },
                    ),
        )
    }

    private fun JsonObject.feedKeysForLog(): String {
        val response = this["response"] as? JsonObject
        val rootKeys = keys.sorted().joinToString(prefix = "[", postfix = "]")
        val responseKeys = response?.keys
            ?.sorted()
            ?.joinToString(prefix = "[", postfix = "]")
            ?: "[]"
        return "rootKeys=$rootKeys responseKeys=$responseKeys"
    }

    private companion object {
        const val TAG = "YaniHomeFeed"
        val FEED_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
