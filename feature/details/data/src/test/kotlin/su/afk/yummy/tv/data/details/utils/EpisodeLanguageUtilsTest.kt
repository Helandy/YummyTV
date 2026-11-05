package su.afk.yummy.tv.data.details.utils

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class EpisodeLanguageUtilsTest {
    @Test
    fun `interface language is passed for the three supported languages`() {
        assertEquals("uk", episodeTextLanguage(Locale.forLanguageTag("uk-UA")))
        assertEquals("en", episodeTextLanguage(Locale.forLanguageTag("en-GB")))
        assertEquals("ru", episodeTextLanguage(Locale.forLanguageTag("ru-RU")))
    }

    @Test
    fun `any other locale falls back to russian`() {
        assertEquals("ru", episodeTextLanguage(Locale.forLanguageTag("de-DE")))
        assertEquals("ru", episodeTextLanguage(Locale.ROOT))
    }
}
