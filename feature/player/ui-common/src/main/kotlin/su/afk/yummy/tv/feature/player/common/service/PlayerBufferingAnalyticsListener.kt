package su.afk.yummy.tv.feature.player.common.service

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.utils.player.isOkCdnHost

/**
 * Диагностика остановок на буферизацию: сколько длилась, с какого источника, при каком профиле и
 * какой запас был, когда началась последняя подкачка.
 *
 * Нужна, чтобы тюнинг буфера опирался на данные с приставки, а не на догадки. Главный вопрос,
 * на который она отвечает: попадают ли остановки на удержание сегмента в прокси Alloha при
 * обновлении сессии (`AllohaStreamProxy.SEGMENT_HOLD_FOR_REFRESH_MS`, 14 с). Если подкачка началась
 * при запасе меньше удержания ([lastLoadBufferedMs]), а остановка идёт рядом с логом прокси про
 * обновление сессии, то минимальный порог профиля слишком мал.
 *
 * Пишет только в [AnalyticsTracker.log] (в дебаге logcat, в релизе no-op), событий в аналитику не
 * шлёт: объём и приватность этого потока решаются отдельно. Читать:
 * `adb logcat -s PlayerBuffering:D AllohaStreamProxy:D`. Описание — `docs/player-buffering.md`.
 *
 * @param currentUriHost хост текущего media item; вызывается на потоке плеера.
 */
@OptIn(UnstableApi::class)
internal class PlayerBufferingAnalyticsListener(
    private val tracker: AnalyticsTracker,
    private val profile: PlayerBufferProfile,
    private val currentUriHost: () -> String?,
) : AnalyticsListener {

    private var playbackState = Player.STATE_IDLE
    private var playWhenReady = false

    /** Начало текущей остановки, или null — сейчас не буферизуемся при запрошенном воспроизведении. */
    private var stallStartedAtMs: Long? = null
    private var stallKind = KIND_REBUFFER
    private var stallBufferedAtLoadMs = 0L
    private var stallSinceLoadMs = 0L

    /** С момента последнего перехода в READY или смены media item: дальше остановки — это ребуфер. */
    private var readySinceMediaItem = false
    private var seekPending = false

    // Запас на старте последней подкачки медиа и время этого старта.
    private var lastLoadBufferedMs = 0L
    private var lastLoadAtMs = 0L

    override fun onMediaItemTransition(
        eventTime: AnalyticsListener.EventTime,
        mediaItem: MediaItem?,
        reason: Int,
    ) {
        readySinceMediaItem = false
        seekPending = false
        lastLoadBufferedMs = 0L
        lastLoadAtMs = 0L
    }

    override fun onPositionDiscontinuity(
        eventTime: AnalyticsListener.EventTime,
        oldPosition: Player.PositionInfo,
        newPosition: Player.PositionInfo,
        reason: Int,
    ) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) seekPending = true
    }

    // Начало подкачки: запас на этот момент — это уровень, с которого загрузчик снова пошёл за
    // данными, то есть реально гарантированный минимум (около minBufferMs профиля).
    override fun onLoadStarted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        retryCount: Int,
    ) {
        // Повторная попытка той же загрузки — не новая подкачка: уровень запаса уже зафиксирован.
        if (retryCount > 0 || mediaLoadData.dataType != C.DATA_TYPE_MEDIA) return
        lastLoadBufferedMs = eventTime.totalBufferedDurationMs
        lastLoadAtMs = SystemClock.elapsedRealtime()
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        playbackState = state
        if (state == Player.STATE_READY) readySinceMediaItem = true
        update(eventTime)
    }

    override fun onPlayWhenReadyChanged(
        eventTime: AnalyticsListener.EventTime,
        playWhenReady: Boolean,
        reason: Int,
    ) {
        this.playWhenReady = playWhenReady
        update(eventTime)
    }

    private fun update(eventTime: AnalyticsListener.EventTime) {
        val stalling = playbackState == Player.STATE_BUFFERING && playWhenReady
        val startedAt = stallStartedAtMs
        when {
            stalling && startedAt == null -> beginStall()
            !stalling && startedAt != null -> endStall(startedAt, eventTime)
        }
    }

    private fun beginStall() {
        val now = SystemClock.elapsedRealtime()
        stallStartedAtMs = now
        stallKind = when {
            seekPending -> KIND_SEEK
            !readySinceMediaItem -> KIND_START
            else -> KIND_REBUFFER
        }
        stallBufferedAtLoadMs = lastLoadBufferedMs
        stallSinceLoadMs = if (lastLoadAtMs == 0L) -1L else now - lastLoadAtMs
    }

    private fun endStall(startedAt: Long, eventTime: AnalyticsListener.EventTime) {
        stallStartedAtMs = null
        seekPending = false
        val durationMs = SystemClock.elapsedRealtime() - startedAt
        // Старт и перемотка буферизуются всегда и не интересны: нужны остановки посреди просмотра.
        if (stallKind != KIND_REBUFFER && durationMs < LOGGED_NON_REBUFFER_MIN_MS) return
        tracker.log(LOG_TAG) {
            "stall kind=$stallKind source=${sourceLabel()} durationMs=$durationMs " +
                "profile=${profile.name}(min=${profile.minBufferMs / 1000}s,max=${profile.maxBufferMs / 1000}s) " +
                "lastLoadBufferedMs=$stallBufferedAtLoadMs lastLoadAgoMs=$stallSinceLoadMs " +
                "positionMs=${eventTime.currentPlaybackPositionMs}"
        }
    }

    private fun sourceLabel(): String {
        val host = currentUriHost() ?: return "unknown"
        return when {
            host == LOOPBACK_HOST -> "alloha-proxy"
            host.isOkCdnHost() -> "okcdn"
            else -> "other"
        }
    }

    private companion object {
        const val LOG_TAG = "PlayerBuffering"
        const val LOOPBACK_HOST = "127.0.0.1"
        const val KIND_START = "start"
        const val KIND_SEEK = "seek"
        const val KIND_REBUFFER = "rebuffer"

        // Долгий старт или перемотка тоже полезны (медленный CDN), короткие — шум.
        const val LOGGED_NON_REBUFFER_MIN_MS = 3_000L
    }
}
