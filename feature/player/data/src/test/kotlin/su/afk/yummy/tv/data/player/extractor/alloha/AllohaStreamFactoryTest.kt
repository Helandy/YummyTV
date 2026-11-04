package su.afk.yummy.tv.data.player.extractor.alloha

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack

/** Поток, с которого стартует плеер: озвучка по умолчанию, её ладдер и ладдеры остальных озвучек. */
class AllohaStreamFactoryTest : BaseUnitTest() {

    private fun track(id: String, default: Boolean, vararg ladder: Pair<String, String>) =
        AllohaAudioTrack(id, "Voice $id", default, linkedMapOf(*ladder))

    private fun opened(vararg tracks: AllohaAudioTrack) = AllohaOpenedSession(
        sources = AllohaParsedSources(
            audioTracks = tracks.toList(),
            subtitles = listOf(AllohaSubtitleTrack("Russian", "https://cdn/ru.vtt", "ru", "vtt")),
        ),
        headers = mapOf("origin" to "https://alloha", "user-agent" to "UA"),
    )

    private val standard = opened(
        track("a", true, "360p" to "https://cdn/a360", "720p" to "https://cdn/a720"),
        track("b", false, "480p" to "https://cdn/b480", "720p" to "https://cdn/b720"),
    )

    @Test
    fun `default dubbing plays at its best quality`() {
        val stream = buildAllohaStream(standard, preferredQualityLabel = null)

        assertEquals("a", stream.selectedAllohaAudioId)
        assertEquals("https://cdn/a720", stream.url)
        assertEquals(listOf("360p", "720p"), stream.qualities!!.keys.toList())
        assertEquals(listOf("Russian"), stream.allohaSubtitles.map { it.label })
    }

    @Test
    fun `every dubbing keeps its own ladder`() {
        val stream = buildAllohaStream(standard, preferredQualityLabel = null)

        assertEquals(listOf("a", "b"), stream.allohaAudioTracks.map { it.id })
        assertEquals(listOf("480p", "720p"), stream.allohaAudioTracks[1].qualities.keys.toList())
    }

    @Test
    fun `preferred quality is used only when the dubbing offers it`() {
        assertEquals("https://cdn/a360", buildAllohaStream(standard, "360p").url)
        assertEquals("https://cdn/a720", buildAllohaStream(standard, "1080p").url)
    }

    @Test
    fun `first dubbing is used when none is marked default`() {
        val stream = buildAllohaStream(
            opened(track("x", false, "720p" to "https://cdn/x720"), track("y", false, "720p" to "https://cdn/y720")),
            preferredQualityLabel = null,
        )

        assertEquals("x", stream.selectedAllohaAudioId)
    }

    @Test
    fun `stream carries the cdn headers for itself and every quality`() {
        val stream = buildAllohaStream(standard, preferredQualityLabel = null)

        assertEquals(mapOf("origin" to "https://alloha", "user-agent" to "UA"), stream.headers)
        assertEquals(setOf("360p", "720p"), stream.qualityHeaders.keys)
        assertTrue(stream.qualityHeaders.values.all { it == stream.headers })
    }
}
