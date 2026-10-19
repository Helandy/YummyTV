package su.afk.yummy.tv.source.anilibria.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AniLibriaExternalIdDto(
    val id: Int? = null,
)

/** Элемент ответа `/app/search/releases` (запрашиваются только нужные поля). */
@Serializable
data class AniLibriaSearchItemDto(
    val id: Int = 0,
    val mal: AniLibriaExternalIdDto? = null,
    val is_blocked_by_geo: Boolean = false,
    val is_blocked_by_copyrights: Boolean = false,
)

@Serializable
data class AniLibriaSkipDto(
    val start: Double? = null,
    val stop: Double? = null,
)

@Serializable
data class AniLibriaEpisodeDto(
    val ordinal: Double? = null,
    val name: String? = null,
    val hls_480: String? = null,
    val hls_720: String? = null,
    val hls_1080: String? = null,
    val duration: Int? = null,
    val opening: AniLibriaSkipDto? = null,
)

/** Ответ `/anime/releases/{id}`. */
@Serializable
data class AniLibriaReleaseDto(
    val id: Int = 0,
    val mal: AniLibriaExternalIdDto? = null,
    val episodes: List<AniLibriaEpisodeDto> = emptyList(),
)
