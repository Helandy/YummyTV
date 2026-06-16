package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activePlayerId
import su.afk.yummy.tv.feature.player.utils.activeVideoId
import javax.inject.Inject

private data class PlayerAnalyticsSource(
    val animeId: Int,
    val videoId: Int,
    val playerId: Int?,
    val episode: String,
    val balancer: String,
    val dubbing: String,
)

internal class PlayerAnalytics @Inject constructor(
    private val tracker: AnalyticsTracker,
) {
    /**
     * Пользователь повторил загрузку потока в плеере.
     *
     * Параметры: screen, action, anime_id.
     */
    fun eventRetryStream(animeId: Int) {
        eventAction(ACTION_RETRY_STREAM, playerParams(animeId))
    }

    /**
     * Пользователь открыл оценку тайтла из плеера.
     *
     * Параметры: screen, action, anime_id.
     */
    fun eventRateTitle(animeId: Int) {
        eventAction(ACTION_RATE_TITLE, playerParams(animeId))
    }

    /**
     * Пользователь перешел к предыдущему эпизоду в плеере.
     *
     * Параметры: screen, action, anime_id.
     */
    fun eventPrevEpisode(animeId: Int) {
        eventAction(ACTION_PREV_EPISODE, playerParams(animeId))
    }

    /**
     * Пользователь перешел к следующему эпизоду в плеере.
     *
     * Параметры: screen, action, anime_id.
     */
    fun eventNextEpisode(animeId: Int) {
        eventAction(ACTION_NEXT_EPISODE, playerParams(animeId))
    }

    /**
     * Пользователь выбрал озвучку в плеере.
     *
     * Параметры: screen, action, anime_id, video_id, episode, player, balancer, dubbing, index,
     * position_ms.
     */
    fun eventDubbingSelected(state: PlayerState.State, index: Int, positionMs: Long) {
        eventAction(
            ACTION_DUBBING_SELECTED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_INDEX to index,
                PARAM_POSITION_MS to positionMs.coerceAtLeast(0L),
            )
        )
    }

    /**
     * Пользователь выбрал балансер в плеере.
     *
     * Параметры: screen, action, anime_id, video_id, episode, player, balancer, dubbing, index,
     * position_ms.
     */
    fun eventBalancerSelected(state: PlayerState.State, index: Int, positionMs: Long) {
        eventAction(
            ACTION_BALANCER_SELECTED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_INDEX to index,
                PARAM_POSITION_MS to positionMs.coerceAtLeast(0L),
            )
        )
    }

    /**
     * Пользователь выбрал качество видео в плеере.
     *
     * Параметры: screen, action, anime_id, quality.
     */
    fun eventQualitySelected(animeId: Int, quality: String) {
        eventAction(
            ACTION_QUALITY_SELECTED,
            playerParams(animeId) + analyticsParamsOf(PARAM_QUALITY to quality)
        )
    }

    /**
     * Пользователь выбрал скорость воспроизведения в плеере.
     *
     * Параметры: screen, action, anime_id, speed.
     */
    fun eventSpeedSelected(animeId: Int, speed: Float) {
        eventAction(
            ACTION_SPEED_SELECTED,
            playerParams(animeId) + analyticsParamsOf(PARAM_SPEED to speed)
        )
    }

    /**
     * Пользователь выбрал режим масштабирования видео в плеере.
     *
     * Параметры: screen, action, anime_id, mode.
     */
    fun eventResizeModeSelected(animeId: Int, mode: PlayerResizeMode) {
        eventAction(
            action = ACTION_RESIZE_MODE_SELECTED,
            params = playerParams(animeId) + analyticsParamsOf(PARAM_MODE to mode.name.lowercase()),
        )
    }

    /**
     * Пользователь выбрал уровень зума видео в плеере.
     *
     * Параметры: screen, action, anime_id, level.
     */
    fun eventZoomLevelSelected(animeId: Int, level: PlayerZoomLevel) {
        eventAction(
            action = ACTION_ZOOM_LEVEL_SELECTED,
            params = playerParams(animeId) + analyticsParamsOf(PARAM_LEVEL to level.name.lowercase()),
        )
    }

    /**
     * Ошибка воспроизведения в плеере.
     *
     * Параметры: screen, anime_id, video_id, player_id, episode, player, balancer, dubbing,
     * error_code, error_type, error_message.
     */
    fun eventPlaybackError(
        state: PlayerState.State,
        message: String,
        errorCode: String?,
        errorType: String?,
    ) {
        tracker.track(
            EVENT_PLAYER_ERROR,
            mapOf(AnalyticsEvents.PARAM_SCREEN to SCREEN_PLAYER) +
                    sourceParams(state.analyticsSource()) +
                    analyticsParamsOf(
                        PARAM_ERROR_CODE to errorCode,
                        PARAM_ERROR_TYPE to errorType,
                        PARAM_ERROR_MESSAGE to message.analyticsMessage(),
                    ),
        )
    }

    /**
     * Плееру не удалось получить playable stream URL из выбранного источника.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, balancer, dubbing, reason,
     * error_type, error_message.
     */
    fun eventStreamResolveFailed(
        state: PlayerState.State,
        reason: String,
        throwable: Throwable? = null,
        message: String? = null,
    ) {
        tracker.track(
            EVENT_STREAM_RESOLVE_FAILED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_REASON to reason,
                PARAM_ERROR_TYPE to throwable?.analyticsType(),
                PARAM_ERROR_MESSAGE to (message ?: throwable?.analyticsMessage()),
            ),
        )
    }

    /**
     * Плеер запустил поток из выбранного источника.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, balancer, dubbing.
     */
    fun eventStreamStarted(state: PlayerState.State) {
        tracker.track(EVENT_STREAM_STARTED, sourceParams(state.analyticsSource()))
    }

    /**
     * Пользователь вручную пропустил opening/ending.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, balancer, dubbing, skip_type,
     * from_ms, to_ms.
     */
    fun eventSkipSegmentSelected(
        state: PlayerState.State,
        type: PlayerSkipType,
        fromMs: Long,
        toMs: Long,
    ) {
        tracker.track(
            EVENT_SKIP_SEGMENT_SELECTED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_SKIP_TYPE to type.analyticsValue(),
                PARAM_FROM_MS to fromMs.coerceAtLeast(0L),
                PARAM_TO_MS to toMs.coerceAtLeast(0L),
            ),
        )
    }

    private fun eventAction(action: String, params: Map<String, String>) {
        tracker.track(
            EVENT_PLAYER_ACTION,
            mapOf(
                AnalyticsEvents.PARAM_SCREEN to SCREEN_PLAYER,
                AnalyticsEvents.PARAM_ACTION to action,
            ) + params,
        )
    }

    private fun playerParams(animeId: Int): Map<String, String> =
        analyticsParamsOf(PARAM_ANIME_ID to animeId.takeIf { it > 0 })

    private fun PlayerState.State.analyticsSource(): PlayerAnalyticsSource =
        PlayerAnalyticsSource(
            animeId = animeId,
            videoId = activeVideoId(this),
            playerId = activePlayerId(this),
            episode = activeEpisode(this),
            balancer = activeBalancerName(this),
            dubbing = activeDubbingName(this),
        )

    private fun sourceParams(source: PlayerAnalyticsSource): Map<String, String> =
        analyticsParamsOf(
            PARAM_ANIME_ID to source.animeId.takeIf { it > 0 },
            PARAM_VIDEO_ID to source.videoId.takeIf { it > 0 },
            PARAM_PLAYER_ID to source.playerId,
            PARAM_EPISODE to source.episode,
            PARAM_PLAYER to source.balancer,
            PARAM_BALANCER to source.balancer,
            PARAM_DUBBING to source.dubbing,
        )

    private fun Throwable.analyticsType(): String =
        this::class.java.simpleName.takeIf { it.isNotBlank() } ?: "unknown"

    private fun Throwable.analyticsMessage(): String? =
        (localizedMessage ?: message)
            ?.analyticsMessage()

    private fun String.analyticsMessage(): String? =
        trim()
            .takeIf { it.isNotBlank() }
            ?.lineSequence()
            ?.joinToString(" ")
            ?.take(MAX_ERROR_MESSAGE_LENGTH)

    private fun PlayerSkipType.analyticsValue(): String =
        when (this) {
            PlayerSkipType.Opening -> "opening"
            PlayerSkipType.Ending -> "ending"
        }

    internal companion object {
        private const val ACTION_BALANCER_SELECTED = "balancer_selected"
        private const val ACTION_DUBBING_SELECTED = "dubbing_selected"
        private const val ACTION_NEXT_EPISODE = "next_episode"
        private const val ACTION_PREV_EPISODE = "prev_episode"
        private const val ACTION_QUALITY_SELECTED = "quality_selected"
        private const val ACTION_RATE_TITLE = "rate_title"
        private const val ACTION_RESIZE_MODE_SELECTED = "resize_mode_selected"
        private const val ACTION_RETRY_STREAM = "retry_stream"
        private const val ACTION_SPEED_SELECTED = "speed_selected"
        private const val ACTION_ZOOM_LEVEL_SELECTED = "zoom_level_selected"
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_BALANCER = "balancer"
        private const val PARAM_DUBBING = "dubbing"
        private const val PARAM_EPISODE = "episode"
        private const val PARAM_ERROR_CODE = "error_code"
        private const val PARAM_ERROR_MESSAGE = "error_message"
        private const val PARAM_ERROR_TYPE = "error_type"
        private const val PARAM_FROM_MS = "from_ms"
        private const val PARAM_INDEX = "index"
        private const val PARAM_LEVEL = "level"
        private const val PARAM_MODE = "mode"
        private const val PARAM_PLAYER = "player"
        private const val PARAM_PLAYER_ID = "player_id"
        private const val PARAM_POSITION_MS = "position_ms"
        private const val PARAM_QUALITY = "quality"
        private const val PARAM_REASON = "reason"
        private const val PARAM_SKIP_TYPE = "skip_type"
        private const val PARAM_SPEED = "speed"
        private const val PARAM_TO_MS = "to_ms"
        private const val PARAM_VIDEO_ID = "video_id"
        private const val MAX_ERROR_MESSAGE_LENGTH = 180
        private const val SCREEN_PLAYER = "player"

        const val EVENT_PLAYER_ACTION = "player_action"
        const val EVENT_PLAYER_ERROR = "player_error"
        const val EVENT_SKIP_SEGMENT_SELECTED = "player_skip_segment_selected"
        const val EVENT_STREAM_STARTED = "player_stream_started"
        const val EVENT_STREAM_RESOLVE_FAILED = "player_stream_resolve_failed"
    }
}
