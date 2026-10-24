package su.afk.yummy.tv.source.anilibria.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AniLibriaExternalIdDto(
    val id: Int? = null,
)

@Serializable
data class AniLibriaNameDto(
    val main: String? = null,
    val english: String? = null,
    val alternative: String? = null,
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
    val name: AniLibriaNameDto? = null,
    val episodes: List<AniLibriaEpisodeDto> = emptyList(),
)

/** Запрос к серверу YummyTV: какие релизы AniLibria у тайтла (сервер хранит копию каталога). */
@Serializable
data class AniLibriaLookupRequestDto(
    val malId: Int,
    val title: String,
    val otherTitles: List<String>,
    val year: Int? = null,
)

/**
 * Ответ сервера YummyTV. [releases] — релизы с уже прочитанными серями (без ссылок на потоки); если их меньше, чем [releaseIds],
 * сервер часть релизов ещё не дочитал.
 */
@Serializable
data class AniLibriaLookupResponseDto(
    val releaseIds: List<Int> = emptyList(),
    val releases: List<AniLibriaCatalogReleaseDto> = emptyList(),
)

@Serializable
data class AniLibriaCatalogReleaseDto(
    val id: Int = 0,
    val dubbing: Boolean = false,
    val episodes: List<AniLibriaCatalogEpisodeDto> = emptyList(),
)

@Serializable
data class AniLibriaCatalogEpisodeDto(
    val ordinal: Int = 0,
    val durationSec: Int? = null,
    val openingStartSec: Int? = null,
    val openingEndSec: Int? = null,
)
