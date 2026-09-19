package su.afk.yummy.tv.data.player.extractor.cvh

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CvhPlaylistSelectionTest {

    private fun item(
        vkId: String,
        voiceStudio: String,
        voiceType: String = "Дубляж",
        episode: Int? = null,
    ) = CvhPlaylistItem(
        vkId = vkId,
        voiceStudio = voiceStudio,
        voiceType = voiceType,
        episode = episode,
    )

    @Test
    fun `movie item without an episode field is still selected`() {
        val movie = item(vkId = "9199690144362", voiceStudio = "ОРТ")

        val selected = selectCvhItem(
            items = listOf(movie),
            isSerial = false,
            episodeNum = 1,
            dubbingCode = "ort",
            dubbingLabel = "Озвучка Дубляж ОРТ",
        )

        assertEquals(movie, selected)
    }

    @Test
    fun `transliterated dubbing code falls back to the dubbing label`() {
        val ort = item(vkId = "ort", voiceStudio = "ОРТ", voiceType = "Дубляж", episode = 1)
        val other = item(vkId = "other", voiceStudio = "AniDUB", voiceType = "Многоголосый", episode = 1)

        val selected = selectCvhItem(
            items = listOf(other, ort),
            isSerial = true,
            episodeNum = 1,
            dubbingCode = "ort",
            dubbingLabel = "Озвучка Дубляж ОРТ",
        )

        assertEquals(ort, selected)
    }

    @Test
    fun `dubbing label without a voice type still matches the studio`() {
        val libria = item(vkId = "libria", voiceStudio = "AniLibria", episode = 1)
        val star = item(vkId = "star", voiceStudio = "AniStar", episode = 1)

        val selected = selectCvhItem(
            items = listOf(star, libria),
            isSerial = true,
            episodeNum = 1,
            dubbingCode = "",
            dubbingLabel = "Озвучка AniLibria",
        )

        assertEquals(libria, selected)
    }

    @Test
    fun `serial picks the requested voice studio and not just the first item`() {
        val star = item(vkId = "star", voiceStudio = "AniStar", episode = 10)
        val libria = item(vkId = "libria", voiceStudio = "AniLibria", episode = 10)
        val dub = item(vkId = "dub", voiceStudio = "AniDUB", voiceType = "Многоголосый", episode = 10)

        val selected = selectCvhItem(
            items = listOf(star, libria, dub),
            isSerial = true,
            episodeNum = 10,
            dubbingCode = "AniLibria",
            dubbingLabel = "Озвучка AniLibria",
        )

        assertEquals(libria, selected)
    }

    @Test
    fun `serial ignores items of other episodes`() {
        val selected = selectCvhItem(
            items = listOf(
                item(vkId = "e9", voiceStudio = "AniLibria", episode = 9),
                item(vkId = "e11", voiceStudio = "AniLibria", episode = 11),
            ),
            isSerial = true,
            episodeNum = 10,
            dubbingCode = "AniLibria",
            dubbingLabel = "Озвучка AniLibria",
        )

        assertNull(selected)
    }

    @Test
    fun `unknown voice falls back to the first candidate of the requested episode`() {
        val firstOfEpisode = item(vkId = "star", voiceStudio = "AniStar", episode = 10)

        val selected = selectCvhItem(
            items = listOf(
                item(vkId = "e9", voiceStudio = "AniLibria", episode = 9),
                firstOfEpisode,
                item(vkId = "dub", voiceStudio = "AniDUB", episode = 10),
            ),
            isSerial = true,
            episodeNum = 10,
            dubbingCode = "SomethingGone",
            dubbingLabel = "Озвучка SomethingGone",
        )

        assertEquals(firstOfEpisode, selected)
    }

    @Test
    fun `blank dubbing hints do not match an empty voice studio`() {
        val subtitles = item(vkId = "subs", voiceStudio = "", voiceType = "Субтитры", episode = 1)
        val libria = item(vkId = "libria", voiceStudio = "AniLibria", episode = 1)

        val selected = selectCvhItem(
            items = listOf(subtitles, libria),
            isSerial = true,
            episodeNum = 1,
            dubbingCode = "AniLibria",
            dubbingLabel = "",
        )

        assertEquals(libria, selected)
    }
}
