package su.afk.yummy.tv.feature.details.details.handler

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetCachedAnimeVideosUseCase
import su.afk.yummy.tv.domain.anime.usecase.RefreshAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.details.model.SubscriptionOption
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.mapper.toDetailsVideosResult
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.utils.resolveDetailsContinueTarget
import su.afk.yummy.tv.feature.details.utils.selectInitialDetailsVideo
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
        pendingSubscriptionStates: Map<String, Boolean> = emptyMap(),
    ): DetailsVideosResult? =
        runSuspendCatching { getCachedAnimeVideos(animeId) }
            .getOrNull()
            ?.toDetailsVideosResult(pendingSubscriptionStates)

    suspend fun load(
        animeId: Int,
        pendingSubscriptionStates: Map<String, Boolean> = emptyMap(),
    ): Result<DetailsVideosResult> =
        runSuspendCatching {
            getAnimeVideos(animeId).toDetailsVideosResult(pendingSubscriptionStates)
        }

    suspend fun refresh(
        animeId: Int,
        pendingSubscriptionStates: Map<String, Boolean> = emptyMap(),
    ): Result<DetailsVideosResult> =
        runSuspendCatching {
            refreshAnimeVideos(animeId).toDetailsVideosResult(pendingSubscriptionStates)
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
