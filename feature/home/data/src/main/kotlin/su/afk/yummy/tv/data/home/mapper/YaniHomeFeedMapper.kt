package su.afk.yummy.tv.data.home.mapper

import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.data.home.R
import su.afk.yummy.tv.data.home.dto.YaniAnimeDto
import su.afk.yummy.tv.data.home.dto.YaniCollectionDto
import su.afk.yummy.tv.data.home.dto.YaniFeedDto
import su.afk.yummy.tv.data.home.dto.YaniPosterDto
import su.afk.yummy.tv.data.home.dto.YaniVideoDto
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomePoster

internal fun YaniFeedDto.toHomeFeed(stringProvider: StringProvider): HomeFeed {
    val heroItems = response.topCarousel.items.mapNotNull { it.toSeriesItem() }
    val newSection = response.newVideos.toNewVideosSection(stringProvider.get(R.string.home_section_new), stringProvider)
    val recommendsSection = response.recommends
        .mapNotNull { it.toSeriesItem() }
        .toItemSection(stringProvider.get(R.string.home_section_recommends))
    val sections = listOfNotNull(
        newSection,
        recommendsSection,
        response.collections.mapNotNull { it.toCollectionItem() }.toItemSection(stringProvider.get(R.string.home_section_collections)),
    )

    return HomeFeed(
        heroItems = heroItems,
        sections = sections,
    )
}

private fun List<YaniVideoDto>.toNewVideosSection(title: String, stringProvider: StringProvider): HomeFeedSection? {
    val items = groupBy { it.animeId }
        .mapNotNull { (animeId, videos) ->
            val id = animeId ?: return@mapNotNull null
            val rep = videos.first()
            val safeTitle = rep.title.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            HomeFeedItem(
                id = id,
                title = safeTitle,
                description = rep.description,
                poster = rep.poster?.toHomePoster(),
                rating = null,
                action = HomeFeedItemAction.OpenSeries(id),
            )
        }
    return items.toItemSection(title)
}

private fun List<HomeFeedItem>.toItemSection(title: String): HomeFeedSection? =
    takeIf { it.isNotEmpty() }?.let { HomeFeedSection(title = title, items = it) }

private fun YaniAnimeDto.toSeriesItem(): HomeFeedItem? {
    val id = animeId ?: return null
    val safeTitle = title.takeIf { it.isNotBlank() } ?: return null

    return HomeFeedItem(
        id = id,
        title = safeTitle,
        description = description,
        poster = poster?.toHomePoster(),
        rating = rating?.average,
        action = HomeFeedItemAction.OpenSeries(id),
    )
}

private fun YaniCollectionDto.toCollectionItem(): HomeFeedItem? {
    val id = id ?: return null
    val safeTitle = title.takeIf { it.isNotBlank() } ?: return null

    return HomeFeedItem(
        id = id.toCollectionDisplayId(),
        title = safeTitle,
        description = description,
        poster = posterPreviews.firstOrNull()?.toHomePoster(),
        rating = null,
        action = HomeFeedItemAction.OpenCollection(id),
    )
}

private fun YaniPosterDto.toHomePoster(): HomePoster = HomePoster(
    small = small?.toHttpsUrl(),
    medium = medium?.toHttpsUrl(),
    big = big?.toHttpsUrl(),
    fullsize = fullsize?.toHttpsUrl(),
    mega = mega?.toHttpsUrl(),
)

private fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}

private fun Int.toCollectionDisplayId(): Int = -this
