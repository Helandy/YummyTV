package su.afk.yummy.tv.data.player.extractor.alloha

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.common.player.AllohaEdgeHash
import java.io.IOException
import java.util.UUID

/** What the player page carries for its HTTP reports: the page's own token, the embedding domain and the file id. */
internal data class AllohaViewTicket(val token: String, val domain: String, val fileId: String)

/** What the stream being played looks like; the reports describe it the way the site's player does. */
internal data class AllohaPlayedStream(
    val manifestUrl: String,
    val audioLabel: String,
    val qualityHeight: Int,
)

/**
 * The website reports a view to Alloha over HTTP next to the WebSocket: `POST /events` (a startup
 * burst, then a heartbeat sample every 10 s, flushed every 30 s) and the legacy `POST /stat` at
 * progress milestones. The startup burst and field set follow what the site's own player was
 * recorded sending (`ad_block_start`, `view_start`, `startup`, `play`, `pause`, `playback_ready`,
 * `ad_block_end`, `play`, `first_frame`, `file_load_complete`).
 *
 * Only what the app really knows is reported (position, duration, stream); the rest stays null.
 */
internal class AllohaViewReporter(
    client: OkHttpClient,
    private val ticket: AllohaViewTicket,
    private val iframeUrl: String,
    private val origin: String,
    private val userAgent: String,
    private val profile: AllohaClientProfile,
    private val stream: AllohaPlayedStream,
    private val positionMs: () -> Long,
    private val durationMs: () -> Long,
    private val analytics: AnalyticsTracker,
) {
    private val http = client.newBuilder().followRedirects(false).retryOnConnectionFailure(false).build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val clientSessionId = newId()
    private val clientRequestId = newId()
    private val playerInitAt = System.currentTimeMillis()
    private val events = JSONArray()
    private val sentPercents = mutableSetOf<Int>()
    private var lastPosition = positionMs()
    private var played = 0.0
    private var lastReportedPlayed = 0.0
    private var lastReportedBytes = 0L
    private var restricted = false
    private var sentEvents = 0
    private var sentStats = 0

    fun start() {
        scope.launch {
            emitStartup()
            flush()
            postStat(percent = null)
            postStat(percent = 0)
            var tick = 0
            while (isActive && !restricted) {
                delay(HEARTBEAT_MS)
                tick++
                sample()
                emitPercentMilestones()
                if (tick % FLUSH_EVERY_TICKS == 0) flush()
            }
        }
    }

    fun close() {
        scope.launch {
            sample()
            emit("pause", paused = true)
            emit("view_finish", "watchTimeSec" to played.toLong(), "completed" to false)
            flush()
            scope.cancel()
        }
        analytics.log(LOG_TAG) { "view reporter closing events=$sentEvents stats=$sentStats" }
    }

    /** The burst the site's player sends when it is ready and starts: paused first, then playing. */
    private fun emitStartup() {
        val sinceInit = System.currentTimeMillis() - playerInitAt
        emit("ad_block_start", paused = true)
        emit(
            "view_start",
            "fileChangeType" to "initial",
            "autoplay" to false,
            "resumeFrom" to positionMs() / 1_000.0,
            "startupQuality" to stream.qualityHeight,
            "startupAudio" to stream.audioLabel,
            paused = true,
        )
        emit(
            "startup",
            "fileChangeType" to "initial",
            "playerStartupMs" to sinceInit,
            paused = true,
        )
        emit("play", paused = true)
        emit("pause", paused = true)
        emit("playback_ready", "timeToPlaybackReadyMs" to sinceInit, paused = true)
        emit("ad_block_end", "adDurationMs" to 0, "adDurationCumulativeMs" to 0, paused = true)
        emit("play", "pauseDurationMs" to 0)
        emit(
            "first_frame",
            "timeToFirstFrameMs" to sinceInit,
            "adDurationMs" to 0,
            "viewStartSource" to "playing",
        )
        emit(
            "file_load_complete",
            "fileChangeType" to "initial",
            "viewStartSource" to "playing",
            "totalFileChangeMs" to sinceInit,
            "adDurationMs" to 0,
            "autoplay" to false,
        )
    }

    private fun sample() {
        val position = positionMs()
        val delta = (position - lastPosition) / 1_000.0
        if (delta > 0 && delta < MAX_STEP_SECONDS) played += delta
        lastPosition = position
        val bytes = AllohaEdgeHash.bytesLoaded.get()
        emit(
            "heartbeat",
            "bytesLoadedTotal" to bytes,
            "bytesLoadedDelta" to (bytes - lastReportedBytes).coerceAtLeast(0L),
            "playedSecondsDelta" to ((played - lastReportedPlayed) * 1_000).toLong() / 1_000.0,
            "isBuffering" to false,
            "isSeeking" to false,
            "playbackRate" to 1,
        )
        lastReportedPlayed = played
        lastReportedBytes = bytes
    }

    private fun emitPercentMilestones() {
        val duration = durationMs()
        if (duration <= 0) return
        val percent = (positionMs() * 100 / duration).toInt()
        for (milestone in PERCENTS) {
            if (percent >= milestone && sentPercents.add(milestone)) {
                emit("view_percent", "percent" to milestone, "watchTimeSec" to played.toLong())
                postStat(percent = milestone)
            }
        }
    }

    @Synchronized
    private fun emit(type: String, vararg extra: Pair<String, Any?>, paused: Boolean = false) {
        val now = System.currentTimeMillis()
        val duration = durationMs()
        val event = JSONObject()
            .put("url", JSONObject.NULL)
            .put("manifestUrl", stream.manifestUrl)
            .put("currentTime", positionMs() / 1_000.0)
            .put("duration", if (duration > 0) duration / 1_000.0 else JSONObject.NULL)
            .put("bufferLength", JSONObject.NULL)
            .put("paused", paused)
            .put("audioTrack", stream.audioLabel)
            .put("quality", stream.qualityHeight)
            .put("subtitle", JSONObject.NULL)
            .put("event_type", type)
            .put("at", now)
            .put("since", now - playerInitAt)
        extra.forEach { (key, value) -> event.put(key, value ?: JSONObject.NULL) }
        events.put(event)
    }

    @Synchronized
    private fun flush() {
        if (events.length() == 0 || restricted) return
        val payload = JSONObject()
            .put("token", ticket.token)
            .put("domain", ticket.domain)
            .put("isTrailer", false)
            .put("dropped", 0)
            .put("playerInitAt", playerInitAt)
            .put("clientSessionId", clientSessionId)
            .put("clientRequestId", clientRequestId)
            .put("env", environment())
            .put("events", JSONArray(events.toString()))
            .toString()
        while (events.length() > 0) events.remove(0)
        val body = FormBody.Builder().add("token", ticket.token).add("payload", payload).build()
        post("/events", body)
        sentEvents++
    }

    private fun postStat(percent: Int?) {
        val body = FormBody.Builder()
            .add("id", ticket.fileId)
            .add("token", ticket.token)
            .add("domain", ticket.domain)
            .add("url", iframeUrl)
            .add("type", "mgpo")
            .add("ab", "false")
        percent?.let { body.add("percent", it.toString()) }
        body.add("info[wasm]", "true")
            .add("info[sw]", "false")
            .add("info[resolution][screenWidth]", profile.screenWidth.toString())
            .add("info[resolution][screenHeight]", profile.screenHeight.toString())
            .add("info[resolution][windowWidth]", profile.windowWidth.toString())
            .add("info[resolution][windowHeight]", profile.windowHeight.toString())
            .add("info[resolution][devicePixelRatio]", jsNumber(profile.pixelRatio))
            .add("info[webSocket]", "true")
            .add("info[platform]", profile.platform)
        sentStats++
        post("/stat", body.build())
    }

    private fun post(path: String, body: FormBody) {
        if (restricted) return
        val request = Request.Builder().url(origin + path).post(body)
            .header("User-Agent", userAgent)
            .header("Origin", origin)
            .header("Referer", iframeUrl)
            .header("Accept-Language", ACCEPT_LANGUAGE)
            .build()
        try {
            http.newCall(request).execute().use { response ->
                analytics.log(LOG_TAG) { "view report $path http=${response.code}" }
                if (response.code == 403 || response.code == 429) restricted = true
            }
        } catch (error: IOException) {
            analytics.log(LOG_TAG) { "view report $path failed ${error.javaClass.simpleName}" }
        }
    }

    private fun environment(): JSONObject = JSONObject()
        .put("connectionType", "4g")
        .put("downlink", DOWNLINK_MBPS)
        .put("pixelRatio", profile.pixelRatio)
        .put("screenW", profile.screenWidth)
        .put("screenH", profile.screenHeight)

    private fun newId(): String = UUID.randomUUID().toString().replace("-", "")

    private companion object {
        const val LOG_TAG = PersistedLogTags.ALLOHA_EXTRACTOR
        const val ACCEPT_LANGUAGE = "ru-RU,ru;q=0.9,en-US;q=0.8,en;q=0.7"
        const val HEARTBEAT_MS = 10_000L
        const val FLUSH_EVERY_TICKS = 3
        const val MAX_STEP_SECONDS = 2.0 * 10
        const val DOWNLINK_MBPS = 1.55
        val PERCENTS = listOf(10, 30, 60, 97)
    }
}
