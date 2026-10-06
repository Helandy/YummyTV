package su.afk.yummy.tv.domain.home.utils

import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem

/**
 * Попадает ли запись библиотеки под источник [source]. Избранное — отдельный флаг, остальные
 * источники сопоставляются со списком аккаунта.
 */
internal fun LibraryItem.matches(source: NewEpisodesSource): Boolean = when (source) {
    NewEpisodesSource.FAVORITES -> isFavorite
    NewEpisodesSource.WATCHING -> listId == UserAnimeList.WATCHING.id
    NewEpisodesSource.PLANNED -> listId == UserAnimeList.PLANNED.id
    NewEpisodesSource.COMPLETED -> listId == UserAnimeList.COMPLETED.id
    NewEpisodesSource.POSTPONED -> listId == UserAnimeList.POSTPONED.id
    NewEpisodesSource.DROPPED -> listId == UserAnimeList.DROPPED.id
}
