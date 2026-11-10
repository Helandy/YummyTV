package su.afk.yummy.tv.external.tenrai.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Страница ответа Jikan-совместимого API `/anime/{malId}/episodes`. */
@Serializable
internal data class TenraiEpisodesDto(
    val data: List<TenraiEpisodeDto> = emptyList(),
    val pagination: TenraiPaginationDto = TenraiPaginationDto(),
)

@Serializable
internal data class TenraiEpisodeDto(
    @SerialName("mal_id") val episodeNumber: Int = 0,
    val filler: Boolean = false,
    val recap: Boolean = false,
)

@Serializable
internal data class TenraiPaginationDto(
    @SerialName("has_next_page") val hasNextPage: Boolean = false,
    @SerialName("last_visible_page") val lastVisiblePage: Int = 0,
)
