package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.feature.home.model.HomeFeedVisibilityParams
import kotlin.random.Random

/** Лента в том виде, в котором её видит пользователь: без скрытого, с новыми сериями. */
internal fun HomeFeed.toVisibleFeed(
    hiddenIds: Set<Int>,
    heroSeed: Long,
    params: HomeFeedVisibilityParams,
    newEpisodesTitle: () -> String,
): HomeFeed {
    val feed = withoutHiddenRecommendations(hiddenIds).withShuffledHero(Random(heroSeed))
    if (!params.newEpisodesEnabled) return feed
    return feed.withMyNewEpisodes(
        recentlyAired = params.recentlyAired,
        libraryAnimeIds = params.libraryAnimeIds,
        watchedEpisodes = params.watchedEpisodes,
        title = newEpisodesTitle(),
    )
}
