package su.afk.yummy.tv.domain.library.utils

import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem

/** Статусы библиотеки, за выходом новых серий которых пользователь активно следит. */
val ACTIVE_LIBRARY_LIST_STATUSES: Set<CardListStatus> = setOf(
    CardListStatus.WATCHING,
    CardListStatus.PLANNED,
)

/** Статус карточки по id списка; `null` для «только избранное» и неизвестных списков. */
fun listIdToCardListStatus(listId: Int): CardListStatus? =
    when (UserAnimeList.entries.firstOrNull { it.id == listId }) {
        UserAnimeList.WATCHING -> CardListStatus.WATCHING
        UserAnimeList.PLANNED -> CardListStatus.PLANNED
        UserAnimeList.COMPLETED -> CardListStatus.COMPLETED
        UserAnimeList.POSTPONED -> CardListStatus.POSTPONED
        UserAnimeList.DROPPED -> CardListStatus.DROPPED
        null -> null
    }

/** Карта `animeId → статус списка` для элементов библиотеки, лежащих в пользовательском списке. */
fun List<LibraryItem>.toCardListStatuses(): Map<Int, CardListStatus> = buildMap {
    for (item in this@toCardListStatuses) {
        val status = listIdToCardListStatus(item.listId) ?: continue
        put(item.animeId, status)
    }
}

/** Карта `animeId → оценка пользователя` для элементов библиотеки с оценкой 1..10. */
fun List<LibraryItem>.toUserRatings(): Map<Int, Int> = buildMap {
    for (item in this@toUserRatings) {
        val rating = item.userRating?.takeIf { it in 1..10 } ?: continue
        put(item.animeId, rating)
    }
}

/**
 * Множество `animeId` тайтлов из указанных пользовательских списков [statuses] и,
 * при [includeFavorites] = `true`, из избранного.
 *
 * По умолчанию учитываются только [ACTIVE_LIBRARY_LIST_STATUSES] («Смотрю» и «Запланировано»)
 * и избранное, чтобы брошенные, отложенные и завершённые тайтлы не попадали в фильтр расписания.
 */
fun List<LibraryItem>.toLibraryAnimeIds(
    statuses: Set<CardListStatus> = ACTIVE_LIBRARY_LIST_STATUSES,
    includeFavorites: Boolean = true,
): Set<Int> = buildSet {
    for (item in this@toLibraryAnimeIds) {
        val status = listIdToCardListStatus(item.listId)
        if ((includeFavorites && item.isFavorite) || (status != null && status in statuses)) {
            add(item.animeId)
        }
    }
}
