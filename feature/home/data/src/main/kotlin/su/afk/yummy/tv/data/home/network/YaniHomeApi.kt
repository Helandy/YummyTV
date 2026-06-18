package su.afk.yummy.tv.data.home.network

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
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
        root.onSuccess { it.logContinueWatchingForDebug() }
    }

    private fun JsonObject.feedKeysForLog(): String {
        val response = this["response"] as? JsonObject
        val lastWatches = response?.get("last_watches") as? JsonArray
        val rootKeys = keys.sorted().joinToString(prefix = "[", postfix = "]")
        val responseKeys = response?.keys
            ?.sorted()
            ?.joinToString(prefix = "[", postfix = "]")
            ?: "[]"
        return "rootKeys=$rootKeys responseKeys=$responseKeys lastWatches=${lastWatches?.size ?: 0}"
    }

    private fun JsonObject.logContinueWatchingForDebug() {
        val lastWatches = (this["response"] as? JsonObject)
            ?.get("last_watches") as? JsonArray
        if (lastWatches == null) {
            Log.d(TAG, "Feed continueWatching raw missing: response.last_watches is absent")
            return
        }
        Log.d(TAG, "Feed continueWatching raw count=${lastWatches.size}")
        lastWatches.forEachIndexed { index, item ->
            Log.d(TAG, "Feed continueWatching raw[$index] ${item.continueWatchingSummaryForLog()}")
        }
    }

    private fun JsonElement.continueWatchingSummaryForLog(): String {
        val item = this as? JsonObject ?: return compactForLog()
        return buildString {
            append("keys=")
            append(item.keys.sorted().joinToString(prefix = "[", postfix = "]"))
            append(" anime_id=")
            append(item["anime_id"].compactForLog())
            append(" video_id=")
            append(item["video_id"].compactForLog())
            append(" title=")
            append(item["title"].compactForLog())
            append(" ep_title=")
            append(item["ep_title"].compactForLog())
            append(" date=")
            append(item["date"].compactForLog())
            append(" end_time=")
            append(item["end_time"].compactForLog())
            append(" duration=")
            append(item["duration"].compactForLog())
            append(" screenshot=")
            append(item["screenshot"].compactForLog())
        }
    }

    private fun JsonElement?.compactForLog(): String =
        when (this) {
            null -> "null"
            is JsonPrimitive -> contentOrNull
                ?.lineSequence()
                ?.joinToString(" ")
                ?.take(LOG_TEXT_LIMIT)
                ?: toString().take(LOG_TEXT_LIMIT)

            else -> toString().lineSequence().joinToString(" ").take(LOG_TEXT_LIMIT)
        }

    private companion object {
        const val TAG = "YaniHomeFeed"
        const val LOG_TEXT_LIMIT = 120
        val FEED_JSON = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}
