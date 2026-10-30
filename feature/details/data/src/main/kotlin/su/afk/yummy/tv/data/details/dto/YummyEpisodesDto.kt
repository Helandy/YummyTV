package su.afk.yummy.tv.data.details.dto

import kotlinx.serialization.Serializable

/**
 * Ответ YummyTV API по серии сезона: /api/anime/mal/{malId}.
 * У серий — [YummyEpisodeDto.previewUrl] (кадр серии из TMDB, w300), у серии 1 ещё [YummyEpisodeDto.stills].
 * Всё необязательное: сервер может не отдать (например, у записи, чьи серии в TMDB идут сквозной нумерацией с другой записью, если не передано смещение).
 */
@Serializable
data class YummyEpisodesDto(
    val malId: Int? = null,
    val tmdbId: Int? = null,
    val seasonNumber: Int? = null,
    /** Смещение, применённое сервером к сериям TMDB; номера в [episodes] уже сдвинуты. */
    val episodeOffset: Int? = null,
    val episodes: List<YummyEpisodeDto> = emptyList(),
)

@Serializable
data class YummyEpisodeDto(
    val episodeNumber: Int = 0,
    val title: String? = null,
    val overview: String? = null,
    val previewUrl: String? = null,
    val stills: List<YummyImageDto>? = null,
)
