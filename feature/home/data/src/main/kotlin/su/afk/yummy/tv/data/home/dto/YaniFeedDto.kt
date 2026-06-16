package su.afk.yummy.tv.data.home.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniFeedDto(
    val response: YaniFeedResponseDto = YaniFeedResponseDto(),
)

@Serializable
data class YaniFeedResponseDto(
    val announcements: List<YaniAnimeDto> = emptyList(),
    @SerialName("top_carousel") val topCarousel: YaniCarouselDto = YaniCarouselDto(),
    val new: List<YaniAnimeDto> = emptyList(),
    val recommends: List<YaniAnimeDto> = emptyList(),
    @SerialName("last_watches") val lastWatches: List<YaniLastWatchDto> = emptyList(),
    @SerialName("new_videos") val newVideos: List<YaniVideoDto> = emptyList(),
    val schedule: List<YaniAnimeDto> = emptyList(),
    val posts: YaniPostsDto = YaniPostsDto(),
    val blogger: YaniBloggerDto = YaniBloggerDto(),
    val collections: List<YaniCollectionDto> = emptyList(),
)

@Serializable
data class YaniLastWatchDto(
    @SerialName("anime_id") val animeId: Int? = null,
    @SerialName("video_id") val videoId: Int? = null,
    @SerialName("anime_url") val animeUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val poster: YaniPosterDto? = null,
    val date: Long? = null,
    @SerialName("end_time") val endTime: Long? = null,
    val duration: Long? = null,
    @SerialName("ep_title") val episodeTitle: String? = null,
    val screenshot: JsonElement? = null,
)

@Serializable
data class YaniCarouselDto(
    val items: List<YaniAnimeDto> = emptyList(),
)

@Serializable
data class YaniAnimeDto(
    @SerialName("anime_id") val animeId: Int? = null,
    @SerialName("anime_url") val animeUrl: String? = null,
    val title: String = "",
    val description: String = "",
    val poster: YaniPosterDto? = null,
    val rating: YaniRatingDto? = null,
)

@Serializable
data class YaniRatingDto(
    val average: Double? = null,
)

@Serializable
data class YaniVideoDto(
    @SerialName("video_id") val videoId: Int? = null,
    @SerialName("anime_id") val animeId: Int? = null,
    @SerialName("anime_url") val animeUrl: String? = null,
    val title: String = "",
    val description: String = "",
    @SerialName("ep_title") val episodeTitle: String? = null,
    @SerialName("dub_title") val dubTitle: String? = null,
    @SerialName("player_title") val playerTitle: String? = null,
    val poster: YaniPosterDto? = null,
)

@Serializable
data class YaniCollectionDto(
    val id: Int? = null,
    val title: String = "",
    val description: String = "",
    @SerialName("poster_previews") val posterPreviews: List<YaniPosterDto> = emptyList(),
)

@Serializable
data class YaniPosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val fullsize: String? = null,
    val mega: String? = null,
)

@Serializable
data class YaniPostsDto(
    val items: List<YaniPostDto> = emptyList(),
)

@Serializable
data class YaniPostDto(
    val id: Int? = null,
    val title: String = "",
)

@Serializable
data class YaniBloggerDto(
    val videos: YaniBloggerVideosDto = YaniBloggerVideosDto(),
)

@Serializable
data class YaniBloggerVideosDto(
    val items: List<YaniBloggerVideoDto> = emptyList(),
)

@Serializable
data class YaniBloggerVideoDto(
    val id: Int? = null,
    val title: String = "",
)
