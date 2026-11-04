package su.afk.yummy.tv.data.player.extractor.alloha

import io.mockk.mockk
import org.json.JSONException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack

class AllohaResponseParsingTest : BaseUnitTest() {

    private val analytics: AnalyticsTracker = mockk(relaxed = true)

    private fun parse(json: String) = parseSources(json, analytics)

    @Test
    fun `response without hls source is unavailable`() {
        val error = assertThrows(AllohaSourceUnavailableException::class.java) { parse("""{"tracks":[]}""") }

        assertEquals("hlsSource is missing", error.message)
    }

    @Test
    fun `sources without any quality ladder are unavailable`() {
        val error = assertThrows(AllohaSourceUnavailableException::class.java) {
            parse("""{"hlsSource":[{"audioId":"a"},{"quality":{}}]}""")
        }

        assertEquals("no HLS qualities found", error.message)
    }

    @Test
    fun `malformed response is a json error`() {
        assertThrows(JSONException::class.java) { parse("<html>") }
    }

    @Test
    fun `ladder is sorted ascending and labels get a p suffix`() {
        val sources = parse(
            """{"hlsSource":[{"audioId":"a","label":"Dub","quality":
                {"1080":"//cdn/1080.m3u8","360":"//cdn/360.m3u8","720p":"//cdn/720.m3u8"}}]}""",
        )

        val ladder = sources.audioTracks.single().qualities
        assertEquals(listOf("360p", "720p", "1080p"), ladder.keys.toList())
        assertEquals("https://cdn/360.m3u8", ladder["360p"])
    }

    @Test
    fun `the mirror picker decides which of the listed urls plays`() {
        val sources = parseSources(
            """{"hlsSource":[{"quality":{"720":"//a/720.m3u8 or //b/720.m3u8","480":"//a/480.m3u8"}}]}""",
            analytics,
            pickMirror = { mirrors -> mirrors.last() },
        )

        val ladder = sources.audioTracks.single().qualities
        assertEquals("https://b/720.m3u8", ladder["720p"])
        assertEquals("https://a/480.m3u8", ladder["480p"])
    }

    @Test
    fun `the picker gets every mirror of a quality normalized`() {
        val seen = mutableListOf<List<String>>()

        parseSources(
            """{"hlsSource":[{"quality":{"720":" //a/720.m3u8  or  //b/720.m3u8 "}}]}""",
            analytics,
            pickMirror = { mirrors -> mirrors.also(seen::add).first() },
        )

        assertEquals(listOf(listOf("https://a/720.m3u8", "https://b/720.m3u8")), seen)
    }

    @Test
    fun `without a picker the first mirror plays`() {
        val sources = parse(
            """{"hlsSource":[{"quality":{"720":"//a/720.m3u8 or //b/720.m3u8"}}]}""",
        )

        assertEquals("https://a/720.m3u8", sources.audioTracks.single().qualities["720p"])
    }

    @Test
    fun `same quality with and without suffix collapses to one entry`() {
        val sources = parse(
            """{"hlsSource":[{"quality":{"720":"//a/first.m3u8","720p":"//a/second.m3u8"}}]}""",
        )

        // Какая из двух ссылок останется, зависит от порядка ключей JSON-библиотеки (на устройстве
        // это порядок документа), поэтому проверяем только схлопывание.
        val ladder = sources.audioTracks.single().qualities
        assertEquals(listOf("720p"), ladder.keys.toList())
        assertTrue(ladder["720p"] in setOf("https://a/first.m3u8", "https://a/second.m3u8"))
    }

    @Test
    fun `json null quality value is not turned into a url`() {
        val sources = parse(
            """{"hlsSource":[{"quality":{"360":null,"720":"//a/720.m3u8"}}]}""",
        )

        assertEquals(listOf("720p"), sources.audioTracks.single().qualities.keys.toList())
    }

    @Test
    fun `audio track falls back to index and numbered label`() {
        val sources = parse(
            """{"hlsSource":[
                {"quality":{"720":"//a/0.m3u8"}},
                {"audioId":"voice-2","label":"  AniLibria  ","default":true,"quality":{"720":"//a/1.m3u8"}}
            ]}""",
        )

        val first = sources.audioTracks[0]
        val second = sources.audioTracks[1]
        assertEquals("0", first.id)
        assertEquals("#1", first.label)
        assertFalse(first.isDefault)
        assertEquals("voice-2", second.id)
        assertEquals("AniLibria", second.label)
        assertTrue(second.isDefault)
    }

    @Test
    fun `entries that are not objects or lack a ladder are skipped but keep their index`() {
        val sources = parse(
            """{"hlsSource":["junk",{"label":"no ladder"},{"quality":{"720":"//a/2.m3u8"}}]}""",
        )

        val track = sources.audioTracks.single()
        assertEquals("2", track.id)
        assertEquals("#3", track.label)
    }

    @Test
    fun `only caption tracks become subtitles`() {
        val sources = parse(
            """{"hlsSource":[{"quality":{"720":"//a/0.m3u8"}}],"tracks":[
                {"kind":"thumbnails","src":"//t/sprite.jpg"},
                {"kind":"Captions","label":"Russian","language":"ru","src":"//t/ru.vtt?token=1"},
                {"kind":"captions","src":"//t/en.SRT"},
                {"kind":"captions","src":""}
            ]}""",
        )

        assertEquals(
            listOf(
                AllohaSubtitleTrack("Russian", "https://t/ru.vtt?token=1", "ru", "vtt"),
                AllohaSubtitleTrack("#3", "https://t/en.SRT", null, "srt"),
            ),
            sources.subtitles,
        )
    }

    @Test
    fun `missing tracks array means no subtitles`() {
        val sources = parse("""{"hlsSource":[{"quality":{"720":"//a/0.m3u8"}}]}""")

        assertTrue(sources.subtitles.isEmpty())
    }

    @Test
    fun `protocol relative stream url gets https`() {
        assertEquals("https://cdn/x.m3u8", "//cdn/x.m3u8".normalizeStreamUrl())
        assertEquals("http://cdn/x.m3u8", "http://cdn/x.m3u8".normalizeStreamUrl())
        assertNull("".normalizeStreamUrl().takeIf(String::isNotBlank))
    }
}
