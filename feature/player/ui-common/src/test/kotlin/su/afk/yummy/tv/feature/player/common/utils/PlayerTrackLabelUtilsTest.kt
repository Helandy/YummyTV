package su.afk.yummy.tv.feature.player.common.utils

import androidx.media3.common.MimeTypes
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import java.util.Locale

class PlayerTrackLabelUtilsTest : BaseUnitTest() {

    private val en = Locale.ENGLISH

    private fun audioLabel(
        language: String? = null,
        label: String? = null,
        mime: String = MimeTypes.AUDIO_AC3,
        channels: Int = 6,
        fallback: String,
        locale: Locale,
    ): String = readableTrackLabel(language, label, mime, channels, fallback, locale)

    @Test
    fun `language title codec and channels are joined`() {
        assertEquals("Russian · Studio Band · AC3 5.1", audioLabel(language = "rus", label = "Studio Band", fallback = "Track 1", locale = en))
    }

    @Test
    fun `label equal to language code is replaced with language name`() {
        assertEquals("Russian · AAC 2.0", audioLabel(language = "rus", label = "rus", mime = MimeTypes.AUDIO_AAC, channels = 2, fallback = "Track 1", locale = en))
    }

    @Test
    fun `title already containing language is not duplicated`() {
        assertEquals("Japanese Dolby · E-AC3 5.1", audioLabel(language = "jpn", label = "Japanese Dolby", mime = MimeTypes.AUDIO_E_AC3, channels = 6, fallback = "Track 1", locale = en))
    }

    @Test
    fun `undetermined language without title falls back but keeps technical info`() {
        assertEquals("Track 2 · AC3 5.1", audioLabel(language = "und", fallback = "Track 2", locale = en))
    }

    @Test
    fun `subtitle track has no technical suffix`() {
        assertEquals("English", readableTrackLabel("eng", null, MimeTypes.TEXT_VTT, -1, "Track 1", en))
    }

    @Test
    fun `duplicate labels get numbers`() {
        assertEquals(
            listOf("A (1)", "B", "A (2)"),
            listOf("A", "B", "A").disambiguated(),
        )
    }
}
