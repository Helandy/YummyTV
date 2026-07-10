package su.afk.yummy.tv.feature.player.handler

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.domain.player.model.AllohaStreamSession
import su.afk.yummy.tv.domain.player.model.PlayerSourceRequest
import su.afk.yummy.tv.domain.player.usecase.GetPlayerSourceGraphUseCase
import su.afk.yummy.tv.feature.player.PlayerAnalytics
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.utils.activeBalancerName
import su.afk.yummy.tv.feature.player.utils.activeDubbingName
import su.afk.yummy.tv.feature.player.utils.activeEpisode
import su.afk.yummy.tv.feature.player.utils.activeIframeUrl
import su.afk.yummy.tv.feature.player.utils.activePlayerId
import su.afk.yummy.tv.feature.player.utils.activeScreenshotUrl
import su.afk.yummy.tv.feature.player.utils.activeVideoId
import su.afk.yummy.tv.feature.player.utils.toPresentationSourceGraph
import javax.inject.Inject

/** Описывает, как обращаться с позицией просмотра при запросе нового потока. */
internal enum class PlayerStreamResumeMode {
    /** Сохраняет текущую позицию, если выбранный источник фактически остается тем же. */
    PreserveCurrent,

    /** Сбрасывает текущую позицию, потому что пользователь явно выбрал другую серию. */
    SelectedSourceOnly,
}

/** Координирует обновление графа источников и получение потока, не владея UI-состоянием. */
internal class PlayerSourceStreamHandler @Inject constructor(
    private val streamHandler: PlayerStreamHandler,
    private val getPlayerSourceGraph: GetPlayerSourceGraphUseCase,
    private val analytics: PlayerAnalytics,
) {
    /**
     * Загружает граф источников для текущего состояния плеера.
     *
     * Возвращает инструкцию вместо мутации состояния, чтобы ViewModel сохраняла контроль над jobs,
     * проверкой destination и применением UI-состояния.
     */
    suspend fun loadSourceGraph(
        state: PlayerState.State,
        forceRefreshVideos: Boolean = false,
        loadStreamOnFailure: Boolean = false,
        loadStreamAfterRefresh: Boolean = false,
        resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
        refreshStreamOnFailure: Boolean = !forceRefreshVideos,
    ): PlayerSourceGraphLoadResult {
        val request = state.toSourceRequest()
        if (request.animeId <= 0) {
            return if (loadStreamOnFailure || loadStreamAfterRefresh) {
                PlayerSourceGraphLoadResult.LoadStream(
                    resumeMode = resumeMode,
                    refreshSourcesOnFailure = refreshStreamOnFailure,
                )
            } else {
                PlayerSourceGraphLoadResult.Ignore
            }
        }

        val sourceGraph = try {
            getPlayerSourceGraph(
                request = request,
                forceRefreshVideos = forceRefreshVideos,
            ).toPresentationSourceGraph()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Throwable) {
            return if (loadStreamOnFailure) {
                PlayerSourceGraphLoadResult.LoadStream(
                    resumeMode = resumeMode,
                    refreshSourcesOnFailure = refreshStreamOnFailure,
                )
            } else {
                PlayerSourceGraphLoadResult.Ignore
            }
        }

        if (sourceGraph.balancers.isEmpty()) return PlayerSourceGraphLoadResult.Ignore

        return PlayerSourceGraphLoadResult.SourceGraph(
            sourceGraph = sourceGraph,
            loadStreamAfterRefresh = loadStreamAfterRefresh,
            resumeMode = resumeMode,
            refreshStreamOnFailure = refreshStreamOnFailure,
        )
    }

    /**
     * Применяет загруженный граф источников.
     *
     * Если после обновления меняется активный iframe, сбрасывает UI-поля текущего потока.
     */
    fun applySourceGraph(
        state: PlayerState.State,
        sourceGraph: PlayerSourceGraph,
    ): PlayerState.State {
        val previousIframeUrl = activeIframeUrl(state)
        val nextState = state.copy(
            sourceGraph = sourceGraph,
            sourceSelection = sourceGraph.selection,
        )
        return if (activeIframeUrl(nextState) != previousIframeUrl) {
            preparingStreamLoad(nextState, PlayerStreamResumeMode.PreserveCurrent)
        } else {
            nextState
        }
    }

    /**
     * Очищает текущий resolved stream перед загрузкой другого источника.
     *
     * В зависимости от [resumeMode] позиция просмотра сохраняется для перезагрузки того же
     * источника или сбрасывается при явном переключении серии.
     */
    fun preparingStreamLoad(
        state: PlayerState.State,
        resumeMode: PlayerStreamResumeMode,
    ): PlayerState.State {
        val preservePlaybackPosition = resumeMode == PlayerStreamResumeMode.PreserveCurrent
        return state.copy(
            streamUrl = null,
            streamHeaders = emptyMap(),
            streamQualityMap = null,
            playerError = null,
            kodikBlockedError = null,
            dubbingResumeMs = if (preservePlaybackPosition) state.dubbingResumeMs else -1L,
            resumeFromMs = if (preservePlaybackPosition) state.resumeFromMs else 0L,
            playbackPositionMs = if (preservePlaybackPosition) state.playbackPositionMs else 0L,
            playbackDurationMs = if (preservePlaybackPosition) state.playbackDurationMs else 0L,
        )
    }

    /** Подготавливает состояние перед resolve активного iframe URL. */
    fun preparingStreamResolve(
        state: PlayerState.State,
        preserveCurrentStream: Boolean = false,
    ): PlayerState.State =
        if (preserveCurrentStream) {
            state.copy(
                playerError = null,
                kodikBlockedError = null,
            )
        } else state.copy(
            streamUrl = null,
            streamHeaders = emptyMap(),
            streamQualityMap = null,
            playerError = null,
            kodikBlockedError = null,
            resumeFromMs = 0L,
            playbackPositionMs = 0L,
            playbackDurationMs = 0L,
        )

    /**
     * Превращает активный iframe в playable stream.
     *
     * При первой ошибке resolve может попросить ViewModel обновить `/videos` и повторить один раз.
     * Когда [refreshSourcesOnFailure] равен false, возвращаемое состояние содержит финальную
     * ошибку для UI.
     */
    suspend fun resolveStream(
        state: PlayerState.State,
        pendingResume: Long?,
        destinationResumeMs: Long?,
        resumeMode: PlayerStreamResumeMode,
        refreshSourcesOnFailure: Boolean,
    ): PlayerStreamLoadResult {
        val result = try {
            streamHandler.resolve(state, pendingResume)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            analytics.eventStreamResolveFailed(
                state = state,
                reason = PlayerStreamResult.REASON_EXCEPTION,
                throwable = exception,
            )
            return if (refreshSourcesOnFailure) {
                PlayerStreamLoadResult.RefreshSources(resumeMode)
            } else {
                PlayerStreamLoadResult.State(
                    state.copy(
                        streamUrl = null,
                        streamHeaders = emptyMap(),
                        streamQualityMap = null,
                        playerError = streamHandler.playbackErrorMessage(
                            exception.localizedMessage ?: exception.message.orEmpty()
                        ),
                    ),
                    consumedDestinationResume = false,
                )
            }
        }

        return when (result) {
            is PlayerStreamResult.Stream -> {
                analytics.eventStreamStarted(state)
                PlayerStreamLoadResult.State(
                    state.copy(
                        streamHeaders = result.headers,
                        streamQualityMap = result.qualities,
                        selectedQuality = result.selectedQuality,
                        streamUrl = result.url,
                        resumeFromMs = result.resumeFromMs,
                        dubbingResumeMs = if (result.consumedPendingResume) {
                            -1L
                        } else {
                            state.dubbingResumeMs
                        },
                    ),
                    consumedDestinationResume =
                        destinationResumeMs != null && pendingResume == destinationResumeMs,
                    allohaSession = result.allohaSession,
                )
            }

            is PlayerStreamResult.KodikBlocked -> {
                analytics.eventStreamResolveFailed(
                    state = state,
                    reason = PlayerStreamResult.REASON_KODIK_BLOCKED,
                    message = result.message,
                )
                if (refreshSourcesOnFailure) {
                    PlayerStreamLoadResult.RefreshSources(resumeMode)
                } else {
                    PlayerStreamLoadResult.State(
                        state.copy(kodikBlockedError = result.message),
                        consumedDestinationResume = false,
                    )
                }
            }

            is PlayerStreamResult.PlayerError -> {
                analytics.eventStreamResolveFailed(
                    state = state,
                    reason = result.reason,
                    message = result.message,
                )
                if (refreshSourcesOnFailure) {
                    PlayerStreamLoadResult.RefreshSources(resumeMode)
                } else {
                    PlayerStreamLoadResult.State(
                        state.copy(playerError = result.message),
                        consumedDestinationResume = false,
                    )
                }
            }
        }
    }

    /** Форматирует ошибки ExoPlayer, не запуская логику обновления источников. */
    fun playbackErrorMessage(message: String): String =
        streamHandler.playbackErrorMessage(message)

    /** Собирает domain-запрос источников из текущего выбранного presentation-источника. */
    private fun PlayerState.State.toSourceRequest(): PlayerSourceRequest =
        PlayerSourceRequest(
            animeId = animeId,
            iframeUrl = activeIframeUrl(this),
            animeTitle = animeTitle,
            episode = activeEpisode(this),
            playerName = activeBalancerName(this),
            dubbing = activeDubbingName(this),
            selectedVideoId = activeVideoId(this),
            selectedPlayerId = activePlayerId(this),
            selectedScreenshotUrl = activeScreenshotUrl(this),
        )
}

/** Инструкция после попытки загрузить или обновить граф источников плеера. */
internal sealed interface PlayerSourceGraphLoadResult {
    /** Граф источников загружен и должен быть применен к текущему состоянию экрана. */
    data class SourceGraph(
        val sourceGraph: PlayerSourceGraph,
        val loadStreamAfterRefresh: Boolean,
        val resumeMode: PlayerStreamResumeMode,
        val refreshStreamOnFailure: Boolean,
    ) : PlayerSourceGraphLoadResult

    /** Граф не удалось или не нужно применять, но получение потока можно продолжить. */
    data class LoadStream(
        val resumeMode: PlayerStreamResumeMode,
        val refreshSourcesOnFailure: Boolean,
    ) : PlayerSourceGraphLoadResult

    /** Ничего менять и загружать не нужно. */
    data object Ignore : PlayerSourceGraphLoadResult
}

/** Инструкция после попытки получить playable stream. */
internal sealed interface PlayerStreamLoadResult {
    /** Поток получен, а поля состояния готовы к merge в состояние экрана. */
    data class State(
        val state: PlayerState.State,
        val consumedDestinationResume: Boolean,
        val allohaSession: AllohaStreamSession? = null,
    ) : PlayerStreamLoadResult

    /** Получение потока один раз упало, вызывающая сторона должна обновить источники и повторить. */
    data class RefreshSources(
        val resumeMode: PlayerStreamResumeMode,
    ) : PlayerStreamLoadResult
}
