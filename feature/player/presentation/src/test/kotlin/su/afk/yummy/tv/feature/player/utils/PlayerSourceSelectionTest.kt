package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection

class PlayerSourceSelectionTest {

    private val graph = PlayerSourceGraph(
        balancers = listOf(
            PlayerSourceBalancer(
                name = "Kodik",
                dubbings = listOf(
                    dubbing("РуАниме / DEEP", 1031101, 1031107),
                    dubbing("Dream Cast", 1031170, 1031178),
                ),
            ),
            PlayerSourceBalancer(
                name = "Alloha",
                dubbings = listOf(dubbing("Dream Cast", 1031520, 1031526)),
            ),
        ),
        // Selection, посчитанный на момент запроса графа, — для другого видео.
        selection = PlayerSourceSelection(balancerIndex = 0, dubbingIndex = 0, episodeIndex = 1),
    )

    @Test
    fun `finds the video inside its balancer and dubbing`() {
        assertEquals(
            PlayerSourceSelection(balancerIndex = 0, dubbingIndex = 1, episodeIndex = 1),
            graph.selectionForVideo(1031178),
        )
    }

    /** Граф пришёл после смены балансера пользователем — выбор не должен откатиться. */
    @Test
    fun `finds the video in another balancer`() {
        assertEquals(
            PlayerSourceSelection(balancerIndex = 1, dubbingIndex = 0, episodeIndex = 0),
            graph.selectionForVideo(1031520),
        )
    }

    @Test
    fun `unknown or missing video id gives no selection`() {
        assertNull(graph.selectionForVideo(0))
        assertNull(graph.selectionForVideo(-1))
        assertNull(graph.selectionForVideo(999))
    }

    private fun dubbing(name: String, vararg ids: Int) = PlayerSourceDubbing(
        name = name,
        episodes = ids.mapIndexed { index, id ->
            PlayerSourceEpisode(id = id, number = (index + 2).toString())
        },
    )
}
