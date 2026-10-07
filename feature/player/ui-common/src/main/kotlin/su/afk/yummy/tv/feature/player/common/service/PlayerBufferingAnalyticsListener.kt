package su.afk.yummy.tv.feature.player.common.service

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.api.PersistedLogTags
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.utils.player.isOkCdnHost
import java.io.IOException

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
 * Пишет только в [AnalyticsTracker.log] (в дебаге logcat, в релизе — в файл логов приложения, тег
 * входит в `PersistedLogTags`), событий в аналитику не шлёт: объём и приватность этого потока
 * решаются отдельно. Ссылок и токенов в строках нет — только метка источника и хост CDN.
 * Читать: `adb logcat -s PlayerBuffering:D AllohaStreamProxy:D` или дамп «Поделиться логами».
 * Описание — `docs/player-buffering.md`.
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

    // Последняя оценка пропускной способности ExoPlayer; в логе остановки показывает, была ли сеть узким местом.
    private var lastBandwidthBps = 0L

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

    override fun onBandwidthEstimate(
        eventTime: AnalyticsListener.EventTime,
        totalLoadTimeMs: Int,
        totalBytesLoaded: Long,
        bitrateEstimate: Long,
    ) {
        lastBandwidthBps = bitrateEstimate
    }

    // Медленная загрузка сегмента — главный подозреваемый в остановках: сам загрузчик Media3 о ней
    // молчит. Быстрые загрузки не пишем, чтобы не шуметь.
    override fun onLoadCompleted(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) {
        if (mediaLoadData.dataType != C.DATA_TYPE_MEDIA) return
        if (loadEventInfo.loadDurationMs < SLOW_LOAD_MS) return
        tracker.log(LOG_TAG) {
            "load slow ${loadEventInfo.describe()} bytes=${loadEventInfo.bytesLoaded} " +
                "loadMs=${loadEventInfo.loadDurationMs} bufferedMs=${eventTime.totalBufferedDurationMs} " +
                "positionMs=${eventTime.currentPlaybackPositionMs}"
        }
    }

    // Отмена долгой загрузки (перерезолв, перемотка) показывает «повисший» сегмент: сколько он
    // висел до отмены. Короткие отмены — обычные перемотки, их не пишем.
    override fun onLoadCanceled(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
    ) {
        if (mediaLoadData.dataType != C.DATA_TYPE_MEDIA) return
        if (loadEventInfo.loadDurationMs < SLOW_LOAD_MS) return
        tracker.log(LOG_TAG) {
            "load canceled ${loadEventInfo.describe()} bytes=${loadEventInfo.bytesLoaded} " +
                "loadMs=${loadEventInfo.loadDurationMs} bufferedMs=${eventTime.totalBufferedDurationMs} " +
                "positionMs=${eventTime.currentPlaybackPositionMs}"
        }
    }

    override fun onLoadError(
        eventTime: AnalyticsListener.EventTime,
        loadEventInfo: LoadEventInfo,
        mediaLoadData: MediaLoadData,
        error: IOException,
        wasCanceled: Boolean,
    ) {
        tracker.log(LOG_TAG) {
            val http = (error as? HttpDataSource.InvalidResponseCodeException)?.responseCode
            "load error type=${mediaLoadData.dataType} ${loadEventInfo.describe()} " +
                "error=${error.javaClass.simpleName} http=${http ?: "-"} canceled=$wasCanceled " +
                "loadMs=${loadEventInfo.loadDurationMs} bufferedMs=${eventTime.totalBufferedDurationMs} " +
                "positionMs=${eventTime.currentPlaybackPositionMs} msg=${error.message?.take(MAX_MESSAGE_CHARS)}"
        }
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        tracker.log(LOG_TAG) {
            "player error code=${error.errorCodeName} cause=${error.cause?.javaClass?.simpleName ?: "-"} " +
                "source=${sourceLabel()} positionMs=${eventTime.currentPlaybackPositionMs} " +
                "msg=${error.message?.take(MAX_MESSAGE_CHARS)}"
        }
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
                "positionMs=${eventTime.currentPlaybackPositionMs} bandwidthBps=$lastBandwidthBps"
        }
    }

    private fun sourceLabel(): String = labelOf(currentUriHost())

    private fun LoadEventInfo.describe(): String {
        val host = uri.host
        return "source=${labelOf(host)} host=${host ?: "-"}"
    }

    private fun labelOf(host: String?): String =
        when {
            host == null -> "unknown"
            host == LOOPBACK_HOST -> "alloha-proxy"
            host.isOkCdnHost() -> "okcdn"
            else -> "other"
        }

    private companion object {
        const val LOG_TAG = PersistedLogTags.PLAYER_BUFFERING
        const val LOOPBACK_HOST = "127.0.0.1"
        const val KIND_START = "start"
        const val KIND_SEEK = "seek"
        const val KIND_REBUFFER = "rebuffer"

        // Долгий старт или перемотка тоже полезны (медленный CDN), короткие — шум.
        const val LOGGED_NON_REBUFFER_MIN_MS = 3_000L

        // Загрузка сегмента дольше этого — медленная (HLS-сегмент Kodik ≈ 18 с видео, запас в
        // профиле SMALL тоже ≈ 18 с); короче — штатная и в лог не идёт.
        const val SLOW_LOAD_MS = 3_000L
        const val MAX_MESSAGE_CHARS = 160
    }
}
