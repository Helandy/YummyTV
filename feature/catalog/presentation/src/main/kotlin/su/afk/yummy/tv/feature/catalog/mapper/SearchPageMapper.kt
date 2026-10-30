package su.afk.yummy.tv.feature.catalog.mapper

import su.afk.yummy.tv.core.utils.paging.OffsetPage
import su.afk.yummy.tv.domain.catalog.model.SearchItem
import su.afk.yummy.tv.domain.catalog.model.SearchPage

internal fun SearchPage.toOffsetPage(): OffsetPage<SearchItem> =
    OffsetPage(
        items = items,
        nextOffset = nextOffset,
        canLoadMore = canLoadMore,
    )
