package su.afk.yummy.tv.feature.player.mapper

import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState

class PlayerPlaybackUiStateMapperTest : BaseUnitTest() {

    @Test
    fun `ошибка из-за сети не называет виноватым плеер`() {
        val state = PlayerState.State(playerError = NO_CONNECTION)

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        assertNull(ui.errorBalancerLabel)
        org.junit.Assert.assertFalse(ui.canChangePlayer)
        org.junit.Assert.assertFalse(ui.canChangeDubbing)
    }

    @Test
    fun `офлайн с несколькими скачанными сериями показывает ленту`() {
        val state = offlineState(episodeCount = 3)

        val ui = state.toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        org.junit.Assert.assertEquals(listOf("1", "2", "3"), ui.episodes.map { it.number })
        org.junit.Assert.assertTrue(ui.episodes.all { it.available })
    }

    @Test
    fun `офлайн с одной скачанной серией ленту не показывает`() {
        val ui = offlineState(episodeCount = 1)
            .toPlayerPlaybackUiState(playerNamePrefix = "Плеер ", connectionErrorMessage = NO_CONNECTION)

        org.junit.Assert.assertTrue(ui.episodes.isEmpty())
    }

    private fun offlineState(episodeCount: Int) = PlayerState.State(
        animeId = 1,
        isOfflinePlayback = true,
        sourceGraph = PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer(
                    name = "kodik",
                    dubbings = listOf(
                        PlayerSourceDubbing(
                            name = "AniLibria",
                            episodes = (1..episodeCount).map {
                                PlayerSourceEpisode(id = it, number = it.toString(), downloadId = it.toLong())
                            },
                        ),
                    ),
                ),
            ),
        ),
        sourceSelection = PlayerSourceSelection(episodeIndex = 0),
    )

    private companion object {
        const val NO_CONNECTION = "Нет подключения к интернету"
    }
}
