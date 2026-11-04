package su.afk.yummy.tv.feature.player.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.feature.player.PlayerState

class PlayerAllohaAudioUtilsTest : BaseUnitTest() {

    private fun ladder(vararg pairs: Pair<String, String>) = linkedMapOf(*pairs)

    private fun state(selectedQuality: String? = "720p") = PlayerState.State(
        allohaAudioTracks = listOf(
            AllohaAudioTrack("a", "Voice A", true, ladder("360p" to "https://cdn/a360", "720p" to "https://cdn/a720")),
            AllohaAudioTrack("b", "Voice B", false, ladder("480p" to "https://cdn/b480", "720p" to "https://cdn/b720")),
            AllohaAudioTrack("empty", "No ladder"),
        ),
        selectedAllohaAudioId = "a",
        streamQualityMap = ladder("360p" to "https://cdn/a360", "720p" to "https://cdn/a720"),
        selectedQuality = selectedQuality,
        streamUrl = "https://cdn/a720",
    )

    @Test
    fun `switching dubbing swaps the ladder and keeps a quality the new dubbing offers`() {
        val switched = state().withAllohaAudio("b")!!

        assertEquals("b", switched.selectedAllohaAudioId)
        assertEquals(listOf("480p", "720p"), switched.streamQualityMap!!.keys.toList())
        assertEquals("720p", switched.selectedQuality)
        assertEquals("https://cdn/b720", switched.streamUrl)
    }

    @Test
    fun `a quality the new dubbing lacks falls back to its best`() {
        val switched = state(selectedQuality = "360p").withAllohaAudio("b")!!

        assertNull(switched.selectedQuality)
        assertEquals("https://cdn/b720", switched.streamUrl)
    }

    @Test
    fun `no chosen quality plays the best of the new dubbing`() {
        val switched = state(selectedQuality = null).withAllohaAudio("b")!!

        assertEquals("https://cdn/b720", switched.streamUrl)
    }

    @Test
    fun `unknown dubbing or one without a ladder changes nothing`() {
        assertNull(state().withAllohaAudio("zzz"))
        assertNull(state().withAllohaAudio("empty"))
    }
}
