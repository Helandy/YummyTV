package su.afk.yummy.tv.feature.details.details.handler

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.SubscriptionOption
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.resolveDetailsContinueTarget
import su.afk.yummy.tv.feature.details.utils.selectInitialDetailsVideo
import su.afk.yummy.tv.feature.details.utils.toSubscriptionOptions
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import javax.inject.Inject

/** Loads details-screen videos and resolves the watch target without mutating UI state. */
internal class DetailsVideoHandler @Inject constructor(
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val getCachedAnimeVideos: GetCachedAnimeVideosUseCase,
    private val refreshAnimeVideos: RefreshAnimeVideosUseCase,
) {
    suspend fun loadCached(
        animeId: Int,
        optimisticSubscriptionKeys: Set<String>,
        optimisticSubscriptionStates: Map<String, Boolean> = emptyMap(),
    ): DetailsVideosResult? =
        runCatching { getCachedAnimeVideos(animeId) }
            .getOrNull()
            ?.toResult(optimisticSubscriptionKeys, optimisticSubscriptionStates)

    suspend fun load(
        animeId: Int,
        optimisticSubscriptionKeys: Set<String>,
        optimisticSubscriptionStates: Map<String, Boolean> = emptyMap(),
    ): Result<DetailsVideosResult> =
        runCatching {
            getAnimeVideos(animeId).toResult(
                optimisticSubscriptionKeys,
                optimisticSubscriptionStates
            )
        }

    suspend fun refresh(
        animeId: Int,
        optimisticSubscriptionKeys: Set<String>,
        optimisticSubscriptionStates: Map<String, Boolean> = emptyMap(),
    ): Result<DetailsVideosResult> =
        runCatching {
            refreshAnimeVideos(animeId).toResult(
                optimisticSubscriptionKeys,
                optimisticSubscriptionStates
            )
        }

    fun resolveWatchTarget(
        animeId: Int,
        videos: List<AnimeVideo>,
        watchProgress: DetailsWatchProgressIndex,
    ): DetailsWatchTarget? {
        val continueTarget = resolveDetailsContinueTarget(
            animeId = animeId,
            videos = videos,
            watchProgress = watchProgress,
        )
        if (continueTarget != null) {
            return DetailsWatchTarget.Continue(continueTarget.video)
        }

        return videos.selectInitialDetailsVideo()?.let { DetailsWatchTarget.Initial(it) }
    }

    private fun List<AnimeVideo>.toResult(
        optimisticSubscriptionKeys: Set<String>,
        optimisticSubscriptionStates: Map<String, Boolean>,
    ): DetailsVideosResult =
        DetailsVideosResult(
            videos = this,
            videosState = if (isEmpty()) VideosUiState.Empty else VideosUiState.Content(this),
            subscriptions = toSubscriptionOptions(
                optimisticKeys = optimisticSubscriptionKeys,
                optimisticStates = optimisticSubscriptionStates,
            ),
        )
}

internal data class DetailsVideosResult(
    val videos: List<AnimeVideo>,
    val videosState: VideosUiState,
    val subscriptions: List<SubscriptionOption>,
)

internal sealed interface DetailsWatchTarget {
    data class Continue(val video: PlayerVideoSource) : DetailsWatchTarget
    data class Initial(val video: AnimeVideo) : DetailsWatchTarget
}
