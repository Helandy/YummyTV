package su.afk.yummy.tv.feature.details.mobile.episodes.model

import su.afk.yummy.tv.core.model.anime.AnimeVideo

internal data class MobileEpisodeGroup(
    val episode: String,
    val video: AnimeVideo,
    val videos: List<AnimeVideo>,
    val kodikIframeUrl: String?,
)
