package su.afk.yummy.tv.feature.library.mapper

import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.utils.listIdToCardListStatus
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.utils.dateText

/**
 * Карточка тайтла библиотеки. Список показываем только на вкладке «Любимые» (на вкладке списка
 * он и так понятен), сердечко — наоборот, только вне «Любимых». [posterUrls] выбирает платформа:
 * у мобилки и ТВ разные правила качества постера.
 */
fun LibraryItem.toTitleListCardUi(tab: LibraryTab, posterUrls: List<String>): TitleListCardUi =
    TitleListCardUi(
        animeId = animeId,
        title = title,
        posterUrls = posterUrls,
        siteRating = rating?.takeIf { it > 0.0 },
        userRating = userRating?.takeIf { it in 1..10 },
        year = year,
        releaseStatus = releaseStatus,
        listStatus = if (tab == LibraryTab.FAVORITES) listIdToCardListStatus(listId) else null,
        isFavorite = isFavorite && tab != LibraryTab.FAVORITES,
        dateText = dateText(tab),
        nextEpisodeAtSeconds = nextEpisodeAtSeconds,
    )
