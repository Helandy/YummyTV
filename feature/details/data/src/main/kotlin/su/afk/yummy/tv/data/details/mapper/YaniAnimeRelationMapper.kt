package su.afk.yummy.tv.data.details.mapper

import kotlinx.serialization.Serializable
import su.afk.yummy.tv.core.utils.poster.animePosterOrNull
import su.afk.yummy.tv.data.details.dto.YaniDirectorResponseDto
import su.afk.yummy.tv.data.details.dto.YaniGenreResponseDto
import su.afk.yummy.tv.data.details.dto.YaniRelatedAnimeDto
import su.afk.yummy.tv.data.details.dto.YaniStudioResponseDto
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationItem
import su.afk.yummy.tv.domain.anime.model.AnimeRelationSubGenre

/**
 * Конверт для кэша relation-данных: хранит одну из метаданных (студия/режиссёр/жанр) вместе со
 * списком связанных аниме и разворачивается в доменную [AnimeRelation] через соответствующий маппер.
 */
@Serializable
internal data class AnimeRelationCachePayload(
    val studio: YaniStudioResponseDto? = null,
    val director: YaniDirectorResponseDto? = null,
    val genre: YaniGenreResponseDto? = null,
    val anime: List<YaniRelatedAnimeDto> = emptyList(),
) {
    fun toDomain(): AnimeRelation = when {
        studio != null -> studio.toAnimeRelation(anime)
        director != null -> director.toAnimeRelation(anime)
        genre != null -> genre.toAnimeRelation(anime)
        else -> error("Cached anime relation has no metadata")
    }
}

internal fun YaniStudioResponseDto.toAnimeRelation(
    anime: List<YaniRelatedAnimeDto>,
) = AnimeRelation(
    title = title,
    anime = anime.toRelationItems(),
)

internal fun YaniDirectorResponseDto.toAnimeRelation(
    anime: List<YaniRelatedAnimeDto>,
) = AnimeRelation(
    title = title,
    secondaryTitle = japaneseTitle.trim().takeIf { it.isNotEmpty() },
    anime = anime.toRelationItems(),
)

internal fun YaniGenreResponseDto.toAnimeRelation(
    anime: List<YaniRelatedAnimeDto>,
) = AnimeRelation(
    title = title,
    description = description.toPlainText().takeIf { it.isNotEmpty() },
    subGenres = subGenres.mapNotNull { genre ->
        val id = genre.id ?: return@mapNotNull null
        val title = genre.title.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        AnimeRelationSubGenre(id = id, title = title, alias = genre.alias)
    },
    anime = anime.toRelationItems(),
)

private fun List<YaniRelatedAnimeDto>.toRelationItems(): List<AnimeRelationItem> =
    mapNotNull { item ->
        val id = item.animeId ?: return@mapNotNull null
        val title = item.title.trim().takeIf { it.isNotEmpty() } ?: return@mapNotNull null
        AnimeRelationItem(
            animeId = id,
            title = title,
            poster = item.poster?.let { animePosterOrNull(it.small, it.medium, it.big, it.fullsize, it.mega) },
            rating = item.rating.average?.takeIf { it > 0.0 },
            year = item.year?.takeIf { it > 0 },
        )
    }.distinctBy { it.animeId }

private fun String.toPlainText(): String =
    replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("</p>", RegexOption.IGNORE_CASE), "\n")
        .replace(Regex("<[^>]+>"), "")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace("&quot;", "\"")
        .replace("&#39;", "'")
        .lines()
        .joinToString("\n") { it.trim() }
        .trim()
