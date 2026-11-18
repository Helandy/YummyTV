package su.afk.yummy.tv.domain.library.model

import su.afk.yummy.tv.domain.account.model.UserAnimeList

/** Запись библиотеки пользователя со связанными внешними ID для экспорта и сверки при импорте. */
data class ExportableLibraryAnime(
    val animeId: Int,
    val title: String,
    val list: UserAnimeList?,
    val isFavorite: Boolean,
    val userRating: Int?,
    /** Слаг типа тайтла с сервера (`tv`, `movie`, `ova`, `ona`, `special`). */
    val typeAlias: String?,
    val shikimoriId: Int?,
    val myAnimeListId: Int?,
)
