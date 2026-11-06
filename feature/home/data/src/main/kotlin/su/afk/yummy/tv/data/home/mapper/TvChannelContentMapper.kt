package su.afk.yummy.tv.data.home.mapper

import su.afk.yummy.tv.core.model.anime.defaultUrl
import su.afk.yummy.tv.core.tv.api.TvChannelContent
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction

/**
 * Проекция элемента главной ленты в карточку системного preview-канала Android TV.
 * Подборки (`OpenCollection`) карточкой канала не представимы — для них возвращается null.
 */
fun HomeFeedItem.toTvChannelContent(): TvChannelContent? {
    val animeId = when (val action = action) {
        is HomeFeedItemAction.OpenSeries -> action.seriesId
        is HomeFeedItemAction.OpenVideo -> action.videoId
        is HomeFeedItemAction.OpenCollection -> return null
    }
    return TvChannelContent(
        animeId = animeId,
        title = title,
        posterUrl = poster.defaultUrl(),
    )
}
