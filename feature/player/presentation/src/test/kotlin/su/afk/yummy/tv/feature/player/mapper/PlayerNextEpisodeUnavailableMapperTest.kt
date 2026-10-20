package su.afk.yummy.tv.feature.player.mapper

import kotlinx.collections.immutable.persistentListOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.model.PlayerFinalEpisodeAction
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeSourceOption
import su.afk.yummy.tv.feature.player.model.PlayerNextEpisodeUnavailable

class PlayerNextEpisodeUnavailableMapperTest : BaseUnitTest() {

    @Test
    fun `one row per dubbing, own dubbing first, current balancer preferred`() {
        val state = state(
            PlayerSourceBalancer(
                name = "Плеер Kodik",
                dubbings = listOf(dubbing("AniLibria", 2, views = 100), dubbing("AniDUB", 3, views = 50)),
            ),
            PlayerSourceBalancer(
                name = "Плеер Alloha",
                dubbings = listOf(dubbing("AniDUB", 3, views = 70), dubbing("AniLibria", 3, views = 120)),
            ),
        )

        val info = state.toPlayerNextEpisodeUnavailable()

        assertEquals(
            listOf(
                PlayerNextEpisodeSourceOption(
                    selection = PlayerSourceSelection(balancerIndex = 1, dubbingIndex = 1, episodeIndex = 2),
                    dubbing = "AniLibria",
                    views = 120,
                    episodeCount = 3,
                    sourceNames = "Alloha",
                ),
                PlayerNextEpisodeSourceOption(
                    // Серия есть и на текущем Kodik — он и выбирается.
                    selection = PlayerSourceSelection(balancerIndex = 0, dubbingIndex = 1, episodeIndex = 2),
                    dubbing = "AniDUB",
                    views = 70,
                    episodeCount = 3,
                    sourceNames = "Kodik • Alloha",
                ),
            ),
            info.options,
        )
        assertEquals("AniLibria", info.currentDubbing)
        assertTrue(info.inCurrentDubbing)
    }

    @Test
    fun `dubbing without the episode anywhere points to other dubbings`() {
        val state = state(
            PlayerSourceBalancer(name = "Плеер Kodik", dubbings = listOf(dubbing("AniStar", 2))),
            PlayerSourceBalancer(name = "Плеер Alloha", dubbings = listOf(dubbing("Dream Cast", 3))),
        )

        val info = state.toPlayerNextEpisodeUnavailable()

        assertEquals(listOf("Dream Cast"), info.options.map { it.dubbing })
        assertFalse(info.inCurrentDubbing)
    }

    @Test
    fun `episode missing everywhere offers a subscription for an ongoing title`() {
        val state = state(
            PlayerSourceBalancer(name = "Плеер Kodik", dubbings = listOf(dubbing("AniLibria", 2))),
        ).copy(finalEpisodeAction = PlayerFinalEpisodeAction.ManageSubscriptions)

        assertEquals(
            PlayerNextEpisodeUnavailable(
                episodeNumber = 3,
                currentDubbing = "AniLibria",
                inCurrentDubbing = false,
                options = persistentListOf(),
                canSubscribe = true,
            ),
            state.toPlayerNextEpisodeUnavailable(),
        )
    }

    @Test
    fun `episode missing everywhere without a subscription just informs`() {
        val state = state(
            PlayerSourceBalancer(name = "Плеер Kodik", dubbings = listOf(dubbing("AniLibria", 2))),
        )

        assertFalse(state.toPlayerNextEpisodeUnavailable().canSubscribe)
    }

    /** Активна последняя серия первой озвучки первого балансера. */
    private fun state(vararg balancers: PlayerSourceBalancer) = PlayerState.State(
        sourceGraph = PlayerSourceGraph(balancers = balancers.toList()),
        sourceSelection = PlayerSourceSelection(
            balancerIndex = 0,
            dubbingIndex = 0,
            episodeIndex = balancers.first().dubbings.first().episodes.lastIndex,
        ),
    )

    private fun dubbing(name: String, episodes: Int, views: Int = 0) = PlayerSourceDubbing(
        name = name,
        views = views,
        episodes = (1..episodes).map { number -> PlayerSourceEpisode(id = number, number = number.toString()) },
    )
}
