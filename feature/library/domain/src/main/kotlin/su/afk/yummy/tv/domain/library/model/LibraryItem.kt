package su.afk.yummy.tv.domain.library.model

import su.afk.yummy.tv.core.model.anime.AnimeSeason

const val FAVORITE_ONLY_LIBRARY_LIST_ID = -1

data class LibraryItem(
    val animeId: Int,
    val title: String,
    val poster: LibraryPoster? = null,
    /** Время добавления; 0 — запись ещё не сохранена, время проставит `UpsertLibraryItemUseCase`. */
    val addedAt: Long = 0L,
    val listId: Int = 0,
    val isFavorite: Boolean = false,
    val listUpdatedAt: Long = addedAt,
    val favoriteUpdatedAt: Long = if (isFavorite) addedAt else 0L,
    val userRating: Int? = null,
    val year: Int? = null,
    /** Общий рейтинг тайтла, приходит вместе со списками пользователя. */
    val rating: Double? = null,
    /** Дата выхода следующей серии, epoch-секунды. */
    val nextEpisodeAtSeconds: Long? = null,
    /** Сезон выхода (квартал года). */
    val season: AnimeSeason? = null,
)
