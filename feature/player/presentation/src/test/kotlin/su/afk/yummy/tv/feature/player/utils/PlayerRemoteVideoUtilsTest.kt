package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState

class PlayerRemoteVideoUtilsTest {
    @Test
    fun `real video keeps its own id`() {
        assertEquals(7, state(selectedBalancer = 1, episodeNumber = "3").remoteVideoId())
    }

    @Test
    fun `4K video takes the same dubbing and episode of a real source`() {
        // "01" у Yani и "1" у 4K — одна серия.
        assertEquals(11, state(selectedBalancer = 0, episodeNumber = "1").remoteVideoId())
    }

    @Test
    fun `falls back to another dubbing of the same episode`() {
        assertEquals(21, state(selectedBalancer = 2, episodeNumber = "5").remoteVideoId())
    }

    @Test
    fun `no real counterpart gives zero`() {
        assertEquals(0, state(selectedBalancer = 0, episodeNumber = "9").remoteVideoId())
    }

    private fun state(selectedBalancer: Int, episodeNumber: String): PlayerState.State {
        val graph = PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer("4K", listOf(dub("AniLibria", episode(-1, episodeNumber)))),
                PlayerSourceBalancer(
                    "Kodik",
                    listOf(dub("AniLibria", episode(7, "3"), episode(11, "01"))),
                ),
                PlayerSourceBalancer("4K2", listOf(dub("Other", episode(-2, episodeNumber)))),
                PlayerSourceBalancer("Kodik2", listOf(dub("Studio", episode(21, "5")))),
            ),
        )
        val index = if (selectedBalancer == 1) 1 else selectedBalancer
        return PlayerState.State(
            sourceGraph = graph,
            sourceSelection = PlayerSourceSelection(balancerIndex = index, dubbingIndex = 0, episodeIndex = 0),
        )
    }

    private fun dub(name: String, vararg episodes: PlayerSourceEpisode) =
        PlayerSourceDubbing(name = name, episodes = episodes.toList())

    private fun episode(id: Int, number: String) = PlayerSourceEpisode(id = id, number = number)
}
