package su.afk.yummy.tv.feature.details.utils

import kotlinx.collections.immutable.toImmutableList
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.details.details.model.BalancerOption
import su.afk.yummy.tv.feature.details.details.model.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.model.DubbingOption
import su.afk.yummy.tv.feature.details.details.model.DubbingPickerState
import su.afk.yummy.tv.feature.details.episodes.dubbings.EpisodeDubbingsState

class SourceStarUtilsTest : BaseUnitTest() {

    @Test
    fun `starred dubbing moves up and returns to its place when unstarred`() {
        val picker = DubbingPickerState(
            episode = "1",
            options = listOf("A", "B", "C").mapIndexed { index, name -> dubbingOption(name, index) }
                .toImmutableList(),
        )

        val starred = picker.withStars(setOf("c"))

        assertEquals(listOf("C", "A", "B"), starred.options.map { it.item.name })
        assertTrue(starred.options.first().item.isStarred)
        val restored = starred.withStars(emptySet())
        assertEquals(listOf("A", "B", "C"), restored.options.map { it.item.name })
        assertFalse(restored.options.any { it.item.isStarred })
    }

    @Test
    fun `starred balancer ignores the player prefix and keeps the order of the rest`() {
        val picker = BalancerPickerState(
            episodeNumber = "1",
            options = listOf("Плеер Kodik", "Плеер Alloha", "Плеер CVH")
                .mapIndexed { index, name -> balancerOption(name, index) }
                .toImmutableList(),
        )

        val starred = picker.withStars(setOf("cvh"))

        assertEquals(listOf("Плеер CVH", "Плеер Kodik", "Плеер Alloha"), starred.options.map { it.playerName })
        assertTrue(starred.options.first().isStarred)
    }

    @Test
    fun `dubbing list puts starred first`() {
        val items = listOf("A", "B").mapIndexed { index, name -> dubbingItem(name, index) }

        assertEquals(listOf("B", "A"), items.withDubbingStars(setOf("b")).map { it.name })
    }

    private fun dubbingItem(name: String, index: Int) = EpisodeDubbingsState.DubbingItem(
        name = name,
        views = 0,
        episodeCount = 1,
        supportedBalancers = "",
        baseIndex = index,
    )

    private fun dubbingOption(name: String, index: Int) =
        DubbingOption(video = video(name), item = dubbingItem(name, index))

    private fun balancerOption(name: String, index: Int) =
        BalancerOption(playerName = name, video = video(name), baseIndex = index)

    private fun video(name: String) = AnimeVideo(
        id = 1,
        episode = "1",
        dubbing = name,
        player = name,
        playerId = null,
        iframeUrl = "https://kodik.example/$name",
        durationSeconds = null,
    )
}
