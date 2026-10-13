package su.afk.yummy.tv.feature.player.handler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.feature.player.PlayerSourceBalancer
import su.afk.yummy.tv.feature.player.PlayerSourceDubbing
import su.afk.yummy.tv.feature.player.PlayerSourceEpisode
import su.afk.yummy.tv.feature.player.PlayerSourceGraph
import su.afk.yummy.tv.feature.player.PlayerSourceSelection
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack

class PlayerSourceSelectionHandlerTest : BaseUnitTest() {

    private val handler = PlayerSourceSelectionHandler()

    private val graph = PlayerSourceGraph(
        balancers = listOf(
            PlayerSourceBalancer(
                name = "Kodik",
                dubbings = listOf(
                    dubbing("AniLibria", views = 10, "1", "2", "3"),
                    dubbing("Dream Cast", views = 50, "1", "2", "3", "4"),
                ),
            ),
            PlayerSourceBalancer(
                name = "Alloha",
                dubbings = listOf(dubbing("AniLibria", views = 10, "2", "3")),
            ),
        ),
    )

    @Test
    fun `previous episode steps back and resets the playback position`() {
        val state = state(PlayerSourceSelection(0, 0, 1)).copy(playbackPositionMs = 5_000L)

        val result = handler.previousEpisode(state)

        assertEquals(PlayerSourceSelection(0, 0, 0), result?.sourceSelection)
        assertEquals(0L, result?.playbackPositionMs)
        assertEquals(-1L, result?.dubbingResumeMs)
    }

    @Test
    fun `no previous episode on the first one`() {
        assertNull(handler.previousEpisode(state(PlayerSourceSelection(0, 0, 0))))
    }

    @Test
    fun `next episode stays inside the active dubbing`() {
        assertEquals(
            PlayerSourceSelection(0, 0, 2),
            handler.nextEpisode(state(PlayerSourceSelection(0, 0, 1)))?.sourceSelection,
        )
        assertNull(handler.nextEpisode(state(PlayerSourceSelection(0, 0, 2))))
    }

    @Test
    fun `next episode in other dubbing prefers the same dubbing on another balancer`() {
        val graph = PlayerSourceGraph(
            balancers = listOf(
                PlayerSourceBalancer("Kodik", listOf(dubbing("A", 0, "1"), dubbing("B", 0, "1", "2"))),
                PlayerSourceBalancer("Alloha", listOf(dubbing("A", 0, "1", "2"))),
            ),
        )
        val state = PlayerState.State(sourceGraph = graph, sourceSelection = PlayerSourceSelection(0, 0, 0))

        assertEquals(
            PlayerSourceSelection(1, 0, 1),
            handler.nextEpisodeInOtherDubbing(state)?.sourceSelection,
        )
    }

    @Test
    fun `next episode in other dubbing falls back to another dubbing on the current balancer`() {
        // Серии 4 в AniLibria нет нигде — берём Dream Cast на том же балансере.
        val state = state(PlayerSourceSelection(0, 0, 2))

        assertEquals(
            PlayerSourceSelection(0, 1, 3),
            handler.nextEpisodeInOtherDubbing(state)?.sourceSelection,
        )
    }

    @Test
    fun `selecting a dubbing keeps the episode number and resumes slightly earlier`() {
        val state = state(PlayerSourceSelection(0, 0, 1)).copy(
            allohaAudioTracks = listOf(AllohaAudioTrack(id = "ru", label = "RU")),
            selectedAllohaAudioId = "ru",
        )
        // Озвучки отсортированы по просмотрам: Dream Cast (50) первая.
        val result = handler.selectDubbing(state, index = 0, currentPosMs = 10_000L)

        assertEquals(PlayerSourceSelection(0, 1, 1), result?.sourceSelection)
        assertEquals(7_000L, result?.dubbingResumeMs)
        assertTrue(result?.allohaAudioTracks.orEmpty().isEmpty())
        assertNull(result?.selectedAllohaAudioId)
    }

    @Test
    fun `selecting a dubbing before playback starts has no resume position`() {
        val result = handler.selectDubbing(state(PlayerSourceSelection(0, 0, 1)), 0, currentPosMs = 0L)

        assertEquals(-1L, result?.dubbingResumeMs)
    }

    @Test
    fun `selecting the current or an unknown dubbing changes nothing`() {
        val state = state(PlayerSourceSelection(0, 0, 0))

        assertNull(handler.selectDubbing(state, index = 1, currentPosMs = 1_000L))
        assertNull(handler.selectDubbing(state, index = 9, currentPosMs = 1_000L))
    }

    @Test
    fun `selecting a dubbing without the current episode changes nothing`() {
        // Серии 4 в AniLibria нет.
        val state = state(PlayerSourceSelection(0, 1, 3))

        assertNull(handler.selectDubbing(state, index = 1, currentPosMs = 1_000L))
    }

    @Test
    fun `selecting a balancer keeps the dubbing and the episode`() {
        val result = handler.selectBalancer(state(PlayerSourceSelection(0, 0, 2)), 1, currentPosMs = 2_000L)

        assertEquals(PlayerSourceSelection(1, 0, 1), result?.sourceSelection)
        assertEquals(0L, result?.dubbingResumeMs)
    }

    @Test
    fun `balancer without the dubbing, the episode or at all is not selected`() {
        assertNull(handler.selectBalancer(state(PlayerSourceSelection(0, 1, 0)), 1, 0L))
        assertNull(handler.selectBalancer(state(PlayerSourceSelection(0, 0, 0)), 1, 0L))
        assertNull(handler.selectBalancer(state(PlayerSourceSelection(0, 0, 0)), 0, 0L))
        assertNull(handler.selectBalancer(state(PlayerSourceSelection(0, 0, 0)), 5, 0L))
    }

    @Test
    fun `resize scope is keyed by the title and the active balancer`() {
        val state = state(PlayerSourceSelection(1, 0, 0)).copy(animeId = ANIME_ID, animeTitle = "Title")

        val scope = handler.resizeSettingsScope(state)

        assertEquals(ANIME_ID, scope.animeId)
        assertEquals("Title", scope.animeTitle)
        assertEquals("Alloha", scope.playerName)
    }

    private fun state(selection: PlayerSourceSelection) =
        PlayerState.State(sourceGraph = graph, sourceSelection = selection)

    private fun dubbing(name: String, views: Int, vararg numbers: String) = PlayerSourceDubbing(
        name = name,
        views = views,
        episodes = numbers.map { number -> PlayerSourceEpisode(id = number.toInt(), number = number) },
    )

    private companion object {
        const val ANIME_ID = 42
    }
}
