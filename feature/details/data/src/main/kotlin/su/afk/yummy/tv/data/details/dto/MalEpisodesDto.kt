package su.afk.yummy.tv.data.details.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Страница ответа Jikan-совместимого API `/anime/{malId}/episodes`; она же — склеенный кэш-документ со всеми сериями.
 * Один документ даёт и пометки филлер/рекап, и картинки серий.
 */
@Serializable
data class MalEpisodesDto(
    val data: List<MalEpisodeDto> = emptyList(),
    val pagination: MalPaginationDto = MalPaginationDto(),
)

@Serializable
data class MalEpisodeDto(
    @SerialName("mal_id") val episodeNumber: Int = 0,
    val filler: Boolean = false,
    val recap: Boolean = false,
    val images: MalEpisodeImagesDto = MalEpisodeImagesDto(),
)

@Serializable
data class MalPaginationDto(
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
)
