package su.afk.yummy.tv.data.details.mapper

import su.afk.yummy.tv.core.model.anime.AnimeTypeKind
import su.afk.yummy.tv.data.details.dto.YaniAnimeTypeDto

/** Слаги типов Yani (`type.alias`), проверены на ответах `/anime/{id}`. */
private val KindsByAlias = mapOf(
    "tv" to AnimeTypeKind.TV,
    "short-tv" to AnimeTypeKind.SHORT_TV,
    "movie" to AnimeTypeKind.MOVIE,
    "short-movie" to AnimeTypeKind.SHORT_MOVIE,
    "ova" to AnimeTypeKind.OVA,
    "ona" to AnimeTypeKind.ONA,
    "special" to AnimeTypeKind.SPECIAL,
)

/** Числовые коды типов Yani (`type.value`) на случай ответа без слага. */
private val KindsByCode = mapOf(
    1 to AnimeTypeKind.TV,
    2 to AnimeTypeKind.MOVIE,
    3 to AnimeTypeKind.SHORT_MOVIE,
    4 to AnimeTypeKind.OVA,
    5 to AnimeTypeKind.SPECIAL,
    6 to AnimeTypeKind.SHORT_TV,
    7 to AnimeTypeKind.ONA,
)

/** Тип Yani → язык-независимая [AnimeTypeKind]: по слагу, а без него по числовому коду; тип «неизвестно» даёт null. */
internal fun YaniAnimeTypeDto?.toAnimeTypeKind(): AnimeTypeKind? {
    val dto = this ?: return null
    val alias = dto.alias?.trim()?.lowercase().orEmpty()
    if (alias == "unknown" || (alias.isEmpty() && (dto.value ?: 0) == 0)) return null
    return KindsByAlias[alias] ?: KindsByCode[dto.value].takeIf { alias.isEmpty() } ?: AnimeTypeKind.OTHER
}
