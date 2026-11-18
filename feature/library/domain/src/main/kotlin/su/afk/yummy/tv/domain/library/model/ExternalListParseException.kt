package su.afk.yummy.tv.domain.library.model

/** Ошибка валидации или разбора файла списка Shikimori / MyAnimeList. */
class ExternalListParseException(
    val error: ExternalListParseError,
    cause: Throwable? = null,
) : IllegalArgumentException(error.name, cause)
