package su.afk.yummy.tv.feature.player

import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingEpisodes
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activePlayerId
import su.afk.yummy.tv.feature.player.utils.activeVideoId
import su.afk.yummy.tv.feature.player.utils.normalizedSourceSelection
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
     * Пользователь открыл экран плеера.
     *
     * Параметры: anime_id.
     */
    fun eventScreenOpened(animeId: Int) {
        tracker.track(EVENT_SCREEN_OPENED, playerParams(animeId))
    }

    /**
     * Пользователь повторил загрузку потока в плеере.
     *
     * Параметры: anime_id.
     */
    fun eventRetryStream(animeId: Int) {
        tracker.track(EVENT_RETRY_STREAM_SELECTED, playerParams(animeId))
    }

    /**
     * Пользователь открыл оценку тайтла из плеера.
     *
     * Параметры: anime_id.
     */
    fun eventRateTitle(animeId: Int) {
        tracker.track(EVENT_RATE_TITLE_SELECTED, playerParams(animeId))
    }

    /** Пользователь открыл управление уведомлениями о новых сериях из плеера. */
    fun eventManageSubscriptions(animeId: Int) {
        tracker.track(EVENT_MANAGE_SUBSCRIPTIONS_SELECTED, playerParams(animeId))
    }

    /**
     * Пользователь открыл тайтл из плеера.
     *
     * Параметры: anime_id.
     */
    fun eventOpenDetails(animeId: Int) {
        tracker.track(EVENT_DETAILS_SELECTED, playerParams(animeId))
    }

    /**
     * Пользователь перешел к предыдущему эпизоду в плеере.
     *
     * Параметры: anime_id.
     */
    fun eventPrevEpisode(animeId: Int) {
        tracker.track(EVENT_PREVIOUS_EPISODE_SELECTED, playerParams(animeId))
    }

    /**
     * Пользователь перешел к следующему эпизоду в плеере.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, dubbing, source.
     */
    fun eventNextEpisode(state: PlayerState.State, source: PlayerNextEpisodeSource) {
        tracker.track(
            EVENT_NEXT_EPISODE_SELECTED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_SOURCE to source.analyticsValue(),
            ),
        )
    }

    /**
     * Пользователь досмотрел эпизод до watched-порога.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, dubbing, position_ms,
     * duration_ms, has_next_episode.
     */
    fun eventEpisodeCompleted(state: PlayerState.State, positionMs: Long, durationMs: Long) {
        tracker.track(
            EVENT_EPISODE_COMPLETED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_POSITION_MS to positionMs.coerceAtLeast(0L),
                PARAM_DURATION_MS to durationMs.coerceAtLeast(0L),
                PARAM_HAS_NEXT_EPISODE to state.hasNextEpisode(),
            ),
        )
    }

    /**
     * Пользователь дошел до физического конца эпизода.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, dubbing, position_ms,
     * duration_ms, has_next_episode.
     */
    fun eventEpisodeFullyCompleted(state: PlayerState.State, positionMs: Long, durationMs: Long) {
        tracker.track(
            EVENT_EPISODE_FULLY_COMPLETED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_POSITION_MS to positionMs.coerceAtLeast(0L),
                PARAM_DURATION_MS to durationMs.coerceAtLeast(0L),
                PARAM_HAS_NEXT_EPISODE to state.hasNextEpisode(),
            ),
        )
    }

    /**
     * Пользователь выбрал озвучку в плеере.
     *
     * Параметры: anime_id, video_id, episode, player, dubbing, index, position_ms.
     */
    fun eventDubbingSelected(state: PlayerState.State, index: Int, positionMs: Long) {
        tracker.track(
            EVENT_DUBBING_SELECTED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_INDEX to index,
                PARAM_POSITION_MS to positionMs.coerceAtLeast(0L),
            )
        )
    }

    /**
     * Пользователь выбрал балансер в плеере.
     *
     * Параметры: anime_id, video_id, episode, player, dubbing, index, position_ms.
     */
    fun eventBalancerSelected(state: PlayerState.State, index: Int, positionMs: Long) {
        tracker.track(
            EVENT_BALANCER_SELECTED,
            sourceParams(state.analyticsSource()) + analyticsParamsOf(
                PARAM_INDEX to index,
                PARAM_POSITION_MS to positionMs.coerceAtLeast(0L),
            )
        )
    }

    /**
     * Пользователь выбрал качество видео в плеере.
     *
     * Параметры: anime_id, quality.
     */
    fun eventQualitySelected(animeId: Int, quality: String) {
        tracker.track(
            EVENT_QUALITY_SELECTED,
            playerParams(animeId) + analyticsParamsOf(PARAM_QUALITY to quality)
        )
    }

    /**
     * Пользователь выбрал скорость воспроизведения в плеере.
     *
     * Параметры: anime_id, speed.
     */
    fun eventSpeedSelected(animeId: Int, speed: Float) {
        tracker.track(
            EVENT_SPEED_SELECTED,
            playerParams(animeId) + analyticsParamsOf(PARAM_SPEED to speed)
        )
    }

    /**
     * Пользователь выбрал режим масштабирования видео в плеере.
     *
     * Параметры: anime_id, mode.
     */
    fun eventResizeModeSelected(animeId: Int, mode: PlayerResizeMode) {
        tracker.track(
            eventName = EVENT_RESIZE_MODE_SELECTED,
            params = playerParams(animeId) + analyticsParamsOf(PARAM_MODE to mode.name.lowercase()),
        )
    }

    /**
     * Пользователь выбрал уровень зума видео в плеере.
     *
     * Параметры: anime_id, level.
     */
    fun eventZoomLevelSelected(animeId: Int, level: PlayerZoomLevel) {
        tracker.track(
            eventName = EVENT_ZOOM_LEVEL_SELECTED,
            params = playerParams(animeId) + analyticsParamsOf(PARAM_LEVEL to level.name.lowercase()),
        )
    }

    /**
     * Ошибка воспроизведения в плеере.
     *
     * Параметры: screen, anime_id, video_id, player_id, episode, player, dubbing,
     * error_code, error_type, error_message.
     */
    fun eventPlaybackError(
        state: PlayerState.State,
        message: String,
        errorCode: String?,
        errorType: String?,
    ) {
        // Ошибки сети — ожидаемый транзиентный кейс, не засоряем крэш-репортинг.
        if (errorCode in IGNORED_ERROR_CODES) return
        val source = state.analyticsSource()
        val errorMessage = message.analyticsMessage()
        tracker.reportError(
            groupIdentifier = EVENT_PLAYER_ERROR,
            message = playerErrorReportMessage(
                eventName = EVENT_PLAYER_ERROR,
                source = source,
                extras = analyticsParamsOf(
                    PARAM_ERROR_CODE to errorCode,
                    PARAM_ERROR_TYPE to errorType,
                    PARAM_ERROR_MESSAGE to errorMessage,
                ),
            ),
            throwable = syntheticPlayerError(
                eventName = EVENT_PLAYER_ERROR,
                message = errorMessage ?: message,
            ),
        )
    }

    /**
     * Плееру не удалось получить playable stream URL из выбранного источника.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, dubbing, reason,
     * error_type, error_message.
     */
    fun eventStreamResolveFailed(
        state: PlayerState.State,
        reason: String,
        throwable: Throwable? = null,
        message: String? = null,
    ) {
        val source = state.analyticsSource()
        val errorType = throwable?.analyticsType()
        val errorMessage = (message ?: throwable?.analyticsMessage())?.analyticsMessage()
        tracker.reportError(
            groupIdentifier = EVENT_STREAM_RESOLVE_FAILED,
            message = playerErrorReportMessage(
                eventName = EVENT_STREAM_RESOLVE_FAILED,
                source = source,
                extras = analyticsParamsOf(
                    PARAM_REASON to reason,
                    PARAM_ERROR_TYPE to errorType,
                    PARAM_ERROR_MESSAGE to errorMessage,
                ),
            ),
            throwable = throwable ?: syntheticPlayerError(
                eventName = EVENT_STREAM_RESOLVE_FAILED,
                message = errorMessage ?: reason,
            ),
        )
    }

    /**
     * Плеер запустил поток из выбранного источника.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, dubbing.
     */
    fun eventStreamStarted(state: PlayerState.State) {
        tracker.track(EVENT_STREAM_STARTED, sourceParams(state.analyticsSource()))
    }

    /**
     * Пользователь вручную пропустил opening/ending.
     *
     * Параметры: anime_id, video_id, player_id, episode, player, dubbing, skip_type,
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
            PARAM_DUBBING to source.dubbing,
        )

    private fun playerErrorReportMessage(
        eventName: String,
        source: PlayerAnalyticsSource,
        extras: Map<String, String>,
    ): String {
        val params = mapOf(
            PARAM_ANIME_ID to source.animeId.toString(),
            PARAM_VIDEO_ID to source.videoId.toString(),
        ) + analyticsParamsOf(
            PARAM_PLAYER_ID to source.playerId,
            PARAM_EPISODE to source.episode,
            PARAM_PLAYER to source.balancer,
            PARAM_DUBBING to source.dubbing,
        ) + extras

        return buildString {
            append("PlayerViewModel: ")
            append(eventName)
            params.forEach { (key, value) ->
                append(", ")
                append(key)
                append("=")
                append(value)
            }
        }
    }

    private fun syntheticPlayerError(eventName: String, message: String): Throwable =
        IllegalStateException("$eventName: ${message.analyticsMessage() ?: "unknown"}")

    private fun PlayerState.State.hasNextEpisode(): Boolean {
        val selection = normalizedSourceSelection(this)
        return selection.episodeIndex < activeDubbingEpisodes(this).lastIndex
    }

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

    private fun PlayerNextEpisodeSource.analyticsValue(): String =
        when (this) {
            PlayerNextEpisodeSource.Controls -> "controls"
            PlayerNextEpisodeSource.EndPrompt -> "end_prompt"
        }

    internal companion object {
        private const val PARAM_ANIME_ID = "anime_id"
        private const val PARAM_DUBBING = "dubbing"
        private const val PARAM_DURATION_MS = "duration_ms"
        private const val PARAM_EPISODE = "episode"
        private const val PARAM_ERROR_CODE = "error_code"
        private const val PARAM_ERROR_MESSAGE = "error_message"
        private const val PARAM_ERROR_TYPE = "error_type"
        private const val PARAM_FROM_MS = "from_ms"
        private const val PARAM_HAS_NEXT_EPISODE = "has_next_episode"
        private const val PARAM_INDEX = "index"
        private const val PARAM_LEVEL = "level"
        private const val PARAM_MODE = "mode"
        private const val PARAM_PLAYER = "player"
        private const val PARAM_PLAYER_ID = "player_id"
        private const val PARAM_POSITION_MS = "position_ms"
        private const val PARAM_QUALITY = "quality"
        private const val PARAM_REASON = "reason"
        private const val PARAM_SKIP_TYPE = "skip_type"
        private const val PARAM_SOURCE = "source"
        private const val PARAM_SPEED = "speed"
        private const val PARAM_TO_MS = "to_ms"
        private const val PARAM_VIDEO_ID = "video_id"
        private const val MAX_ERROR_MESSAGE_LENGTH = 180

        private val IGNORED_ERROR_CODES = setOf(
            "ERROR_CODE_IO_NETWORK_CONNECTION_FAILED",
            "ERROR_CODE_IO_BAD_HTTP_STATUS",
        )

        const val EVENT_BALANCER_SELECTED = "player_balancer_selected"
        const val EVENT_DETAILS_SELECTED = "player_details_selected"
        const val EVENT_DUBBING_SELECTED = "player_dubbing_selected"
        const val EVENT_NEXT_EPISODE_SELECTED = "player_next_episode_selected"
        const val EVENT_PREVIOUS_EPISODE_SELECTED = "player_previous_episode_selected"
        const val EVENT_QUALITY_SELECTED = "player_quality_selected"
        const val EVENT_RATE_TITLE_SELECTED = "player_rate_title_selected"
        const val EVENT_MANAGE_SUBSCRIPTIONS_SELECTED = "player_manage_subscriptions_selected"
        const val EVENT_RESIZE_MODE_SELECTED = "player_resize_mode_selected"
        const val EVENT_RETRY_STREAM_SELECTED = "player_retry_stream_selected"
        const val EVENT_SCREEN_OPENED = "player_screen"
        const val EVENT_EPISODE_COMPLETED = "player_episode_completed"
        const val EVENT_EPISODE_FULLY_COMPLETED = "player_episode_fully_completed"
        const val EVENT_PLAYER_ERROR = "player_error"
        const val EVENT_SKIP_SEGMENT_SELECTED = "player_skip_segment_selected"
        const val EVENT_SPEED_SELECTED = "player_speed_selected"
        const val EVENT_STREAM_STARTED = "player_stream_started"
        const val EVENT_STREAM_RESOLVE_FAILED = "player_stream_resolve_failed"
        const val EVENT_ZOOM_LEVEL_SELECTED = "player_zoom_level_selected"
    }
}
