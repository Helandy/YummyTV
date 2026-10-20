package su.afk.yummy.tv.domain.library.utils

import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem

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
