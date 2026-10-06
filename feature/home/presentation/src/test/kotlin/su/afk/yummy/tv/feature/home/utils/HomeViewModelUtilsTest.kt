package su.afk.yummy.tv.feature.home.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.model.HomeFeedSection
import su.afk.yummy.tv.domain.home.model.HomeFeedSectionType

class HomeViewModelUtilsTest : BaseUnitTest() {

    @Test
    fun `only titles from the library reach the section`() {
        val feed = feed().withMyNewEpisodes(
            recentlyAired = listOf(airedItem(1), airedItem(2)),
            libraryAnimeIds = setOf(2),
            watchedEpisodes = emptyMap(),
            title = TITLE,
        )

        assertEquals(listOf(2), feed.sections.first().items.map { it.id })
    }

    @Test
    fun `the section goes first in the feed`() {
        val feed = feed().withMyNewEpisodes(
            recentlyAired = listOf(airedItem(1)),
            libraryAnimeIds = setOf(1),
            watchedEpisodes = emptyMap(),
            title = TITLE,
        )

        assertEquals(HomeFeedSectionType.MY_NEW_EPISODES, feed.sections.first().type)
        assertEquals(TITLE, feed.sections.first().title)
        assertEquals(listOf(HomeFeedSectionType.NEW_RELEASES), feed.sections.drop(1).map { it.type })
    }

    @Test
    fun `the feed is untouched when nothing matches`() {
        val original = feed()

        val result = original.withMyNewEpisodes(
            recentlyAired = listOf(airedItem(1)),
            libraryAnimeIds = setOf(999),
            watchedEpisodes = emptyMap(),
            title = TITLE,
        )

        assertEquals(original, result)
    }

    @Test
    fun `a watched episode is marked on the card`() {
        val feed = feed().withMyNewEpisodes(
            recentlyAired = listOf(airedItem(1, episodeNumber = 3)),
            libraryAnimeIds = setOf(1),
            watchedEpisodes = mapOf(1 to setOf(1, 2, 3)),
            title = TITLE,
        )

        assertTrue(feed.sections.first().items.single().isWatched)
    }

    @Test
    fun `an earlier watched episode does not mark the new one`() {
        val feed = feed().withMyNewEpisodes(
            recentlyAired = listOf(airedItem(1, episodeNumber = 3)),
            libraryAnimeIds = setOf(1),
            watchedEpisodes = mapOf(1 to setOf(1, 2)),
            title = TITLE,
        )

        assertFalse(feed.sections.first().items.single().isWatched)
    }

    @Test
    fun `a title without an episode number is never marked watched`() {
        val feed = feed().withMyNewEpisodes(
            recentlyAired = listOf(airedItem(1, episodeNumber = null)),
            libraryAnimeIds = setOf(1),
            watchedEpisodes = mapOf(1 to setOf(1, 2, 3)),
            title = TITLE,
        )

        assertFalse(feed.sections.first().items.single().isWatched)
    }

    private fun feed() = HomeFeed(
        heroItems = emptyList(),
        sections = listOf(
            HomeFeedSection(HomeFeedSectionType.NEW_RELEASES, "New", listOf(airedItem(50))),
        ),
    )

    private fun airedItem(id: Int, episodeNumber: Int? = 1) = HomeFeedItem(
        id = id,
        title = "Title $id",
        description = "",
        poster = null,
        rating = null,
        year = null,
        action = HomeFeedItemAction.OpenSeries(id),
        episodeNumber = episodeNumber,
        airedAtSeconds = 1_791_000_000L,
    )

    private companion object {
        const val TITLE = "New episodes"
    }
}
