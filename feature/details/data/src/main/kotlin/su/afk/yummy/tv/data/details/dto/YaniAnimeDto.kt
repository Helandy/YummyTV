package su.afk.yummy.tv.data.details.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class YaniAnimeDetailsDto(val response: YaniAnimeResponseDto = YaniAnimeResponseDto())

@Serializable
data class YaniAnimeResponseDto(
    @SerialName("anime_id") val animeId: Int? = null,
    @SerialName("anime_url") val animeUrl: String = "",
    val title: String = "",
    val description: String = "",
    val poster: YaniAnimePosterDto? = null,
    val rating: YaniAnimeRatingDto = YaniAnimeRatingDto(),
    val genres: List<YaniNamedDto> = emptyList(),
    val year: Int? = null,
    @SerialName("min_age") val minAge: YaniAgeRatingDto? = null,
    val views: Int? = null,
    val season: Int? = null,
    @SerialName("anime_status") val animeStatus: YaniAliasTitleDto? = null,
    val type: YaniAnimeTypeDto? = null,
    val episodes: YaniEpisodesDto? = null,
    @SerialName("other_titles") val otherTitles: List<String> = emptyList(),
    val creators: List<YaniNamedDto> = emptyList(),
    val studios: List<YaniNamedDto> = emptyList(),
    @SerialName("viewing_order") val viewingOrder: List<YaniViewingOrderItemDto> = emptyList(),
    @SerialName("random_screenshots") val randomScreenshots: List<YaniScreenshotDto> = emptyList(),
)

@Serializable
data class YaniAnimePosterDto(
    val small: String? = null,
    val medium: String? = null,
    val big: String? = null,
    val fullsize: String? = null,
    val mega: String? = null,
)

@Serializable
data class YaniAnimeRatingDto(
    val average: Double? = null,
    val counters: Int? = null,
    @SerialName("kp_rating") val kinopoisk: Double? = null,
    @SerialName("shikimori_rating") val shikimori: Double? = null,
    @SerialName("myanimelist_rating") val myAnimeList: Double? = null,
)

@Serializable
data class YaniNamedDto(val id: Int? = null, val title: String = "")

@Serializable
data class YaniAgeRatingDto(val title: String? = null, @SerialName("title_long") val titleLong: String? = null)

@Serializable
data class YaniAliasTitleDto(val title: String? = null, val alias: String? = null)

@Serializable
data class YaniAnimeTypeDto(val name: String? = null, val shortname: String? = null, val value: Int? = null)

@Serializable
data class YaniEpisodesDto(
    val count: Int? = null,
    val aired: Int? = null,
    @SerialName("next_date") val nextDate: Long? = null,
    @SerialName("prev_date") val prevDate: Long? = null,
)

@Serializable
data class YaniViewingOrderItemDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val data: YaniViewingOrderDataDto? = null,
    val type: YaniAnimeTypeDto? = null,
    val poster: YaniAnimePosterDto? = null,
    val year: Int? = null,
    val rating: Double? = null,
)

@Serializable
data class YaniViewingOrderDataDto(val text: String? = null)

@Serializable
data class YaniScreenshotDto(
    val id: Int? = null,
    val episode: String? = null,
    val sizes: YaniScreenshotSizesDto = YaniScreenshotSizesDto(),
)

@Serializable
data class YaniScreenshotSizesDto(val small: String? = null, val full: String? = null)

@Serializable
data class YaniAnimeVideosDto(val response: List<YaniAnimeVideoDto> = emptyList())

@Serializable
data class YaniAnimeVideoDto(
    @SerialName("video_id") val videoId: Int = 0,
    val data: YaniVideoDataDto = YaniVideoDataDto(),
    val number: String = "",
    @SerialName("iframe_url") val iframeUrl: String = "",
    val duration: Int? = null,
    val views: Int? = null,
    val skips: YaniVideoSkipsDto? = null,
)

@Serializable
data class YaniVideoDataDto(
    val player: String = "",
    val dubbing: String = "",
    @SerialName("player_id") val playerId: Int? = null,
)

@Serializable
data class YaniVideoSkipsDto(
    val opening: JsonElement? = null,
    val ending: JsonElement? = null,
)

@Serializable
data class YaniRecommendationsDto(val response: List<YaniRecommendationItemDto> = emptyList())

@Serializable
data class YaniRecommendationItemDto(
    @SerialName("anime_id") val animeId: Int? = null,
    val title: String = "",
    val poster: YaniAnimePosterDto? = null,
    val rating: YaniAnimeRatingDto = YaniAnimeRatingDto(),
    val type: YaniAnimeTypeDto? = null,
    val year: Int? = null,
)

@Serializable
data class YaniTrailersResponseDto(val response: List<YaniTrailerDto> = emptyList())

@Serializable
data class YaniTrailerDto(@SerialName("iframe_url") val iframeUrl: String = "")
