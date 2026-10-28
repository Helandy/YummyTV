package su.afk.yummy.tv.feature.library.utils

import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toImmutableMap
import su.afk.yummy.tv.core.model.settings.LibrarySort
import su.afk.yummy.tv.core.model.settings.LibrarySortDirection
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.model.LibraryTab

internal fun LibraryTab.userAnimeList(): UserAnimeList? = when (this) {
    LibraryTab.WATCHING -> UserAnimeList.WATCHING
    LibraryTab.PLANNED -> UserAnimeList.PLANNED
    LibraryTab.COMPLETED -> UserAnimeList.COMPLETED
    LibraryTab.POSTPONED -> UserAnimeList.POSTPONED
    LibraryTab.DROPPED -> UserAnimeList.DROPPED
    LibraryTab.CONTINUE_WATCHING,
    LibraryTab.HISTORY,
    LibraryTab.FAVORITES -> null
}

/**
 * Данные вкладки ещё не прочитаны из локального хранилища — показывать лоадер, а не «Список пуст».
 * У истории своё состояние загрузки в Paging, поэтому она здесь не учитывается.
 */
fun LibraryState.State.isLocalLoading(tab: LibraryTab): Boolean = when (tab) {
    LibraryTab.CONTINUE_WATCHING -> !isContinueWatchingLoaded
    LibraryTab.HISTORY -> false
    LibraryTab.FAVORITES,
    LibraryTab.WATCHING,
    LibraryTab.PLANNED,
    LibraryTab.COMPLETED,
    LibraryTab.POSTPONED,
    LibraryTab.DROPPED -> !isLibraryLoaded
}

/** Раскладывает элементы библиотеки по вкладкам, чтобы UI не занимался фильтрацией. */
internal fun buildLibraryTabItems(
    items: List<LibraryItem>,
    sort: LibrarySort,
    direction: LibrarySortDirection,
    nowEpochSeconds: Long = System.currentTimeMillis() / MILLIS_PER_SECOND,
): ImmutableMap<LibraryTab, ImmutableList<LibraryItem>> =
    LibraryTab.entries.associateWith { tab ->
        when (tab) {
            LibraryTab.CONTINUE_WATCHING, LibraryTab.HISTORY -> emptyList()
            LibraryTab.FAVORITES -> items.filter { it.isFavorite }
            LibraryTab.WATCHING,
            LibraryTab.PLANNED,
            LibraryTab.COMPLETED,
            LibraryTab.POSTPONED,
            LibraryTab.DROPPED -> {
                val localListId = tab.userAnimeList()?.id
                items.filter { it.listId == localListId }
            }
        }
            // Синк списков может отдать два ряда на один тайтл — в гриде это дубль ключа и краш.
            .distinctBy { it.animeId }
            .sortedWith(librarySortComparator(tab, sort, direction, nowEpochSeconds))
            .toImmutableList()
    }.toImmutableMap()

/**
 * Значение, по которому упорядочен список. `null` означает «данных нет» — такие элементы всегда
 * уезжают в конец, иначе при возрастающем порядке список начинался бы с пустых карточек.
 */
private fun LibraryItem.librarySortKey(
    tab: LibraryTab,
    sort: LibrarySort,
    nowEpochSeconds: Long,
): Double? = when (sort) {
    LibrarySort.ADDED_DATE -> {
        val addedAt = if (tab == LibraryTab.FAVORITES) favoriteUpdatedAt else listUpdatedAt
        addedAt.takeIf { it > 0L }?.toDouble()
    }

    LibrarySort.YEAR -> year?.toDouble()
    LibrarySort.RATING -> rating?.takeIf { it > 0.0 }
    LibrarySort.USER_RATING -> userRating?.takeIf { it in 1..10 }?.toDouble()
    // «Сначала главное» для выхода серии — ближайшая дата, поэтому ключ отрицательный.
    // Уже прошедшая дата устарела (серия вышла) и ключа не даёт.
    LibrarySort.NEXT_EPISODE -> nextEpisodeAtSeconds
        ?.takeIf { it > nowEpochSeconds }
        ?.let { -it.toDouble() }

    LibrarySort.TITLE -> null
}

/** При равных ключах порядок добивается названием, чтобы список не «прыгал» между пересборками. */
private fun librarySortComparator(
    tab: LibraryTab,
    sort: LibrarySort,
    direction: LibrarySortDirection,
    nowEpochSeconds: Long,
): Comparator<LibraryItem> = Comparator { first, second ->
    val byTitle = first.title.compareTo(second.title, ignoreCase = true)
    if (sort == LibrarySort.TITLE) {
        // Стрелка «вниз» везде означает «сначала главное»: новее, больше год, выше рейтинг —
        // и для названия это начало алфавита, а не его конец.
        return@Comparator if (direction == LibrarySortDirection.DESC) byTitle else -byTitle
    }
    val firstKey = first.librarySortKey(tab, sort, nowEpochSeconds)
    val secondKey = second.librarySortKey(tab, sort, nowEpochSeconds)
    val byKey = when {
        firstKey == null && secondKey == null -> 0
        firstKey == null -> 1
        secondKey == null -> -1
        direction == LibrarySortDirection.DESC -> secondKey.compareTo(firstKey)
        else -> firstKey.compareTo(secondKey)
    }
    if (byKey != 0) byKey else byTitle
}

private const val MILLIS_PER_SECOND = 1_000L
