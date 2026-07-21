package su.afk.yummy.tv.feature.details.utils

import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.details.handler.DetailsVideosResult

internal fun List<AnimeVideo>.toDetailsVideosResult(
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
