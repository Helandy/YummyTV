package su.afk.yummy.tv.feature.player.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaAudioTrack
import su.afk.yummy.tv.domain.player.model.AllohaSubtitleTrack
import su.afk.yummy.tv.domain.player.model.AllohaTrackPreference
import su.afk.yummy.tv.domain.player.usecase.GetAllohaTrackPreferenceUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveAllohaAudioSelectionUseCase
import su.afk.yummy.tv.domain.player.usecase.SaveAllohaSubtitleSelectionUseCase

class PlayerAllohaTrackPreferenceHandlerTest : BaseUnitTest() {

    private val getPreference: GetAllohaTrackPreferenceUseCase = mockk()
    private val saveAudio: SaveAllohaAudioSelectionUseCase = mockk(relaxed = true)
    private val saveSubtitle: SaveAllohaSubtitleSelectionUseCase = mockk(relaxed = true)

    private val audio = listOf(AllohaAudioTrack("a1", "Русский"), AllohaAudioTrack("a2", "Japanese"))
    private val subtitles = listOf(
        AllohaSubtitleTrack(label = "Crunchyroll", url = "u0", language = "ru"),
        AllohaSubtitleTrack(label = "Fansub", url = "u1", language = "ru"),
        AllohaSubtitleTrack(label = "English", url = "u2", language = "en"),
    )

    @Test
    fun `no saved preference gives no match`() = runTest {
        stored(null)

        assertNull(findMatch())
    }

    @Test
    fun `audio is matched by label ignoring the regenerated id and case`() = runTest {
        stored(preference(audioLabel = "japanese"))

        assertEquals("a2", findMatch()?.audioId)
    }

    @Test
    fun `ambiguous audio label is not guessed`() = runTest {
        stored(preference(audioLabel = "Русский"))

        val match = findMatch(audioTracks = audio + AllohaAudioTrack("a3", "Русский"))

        assertNull(match?.audioId)
    }

    @Test
    fun `subtitles switched off are applied as off`() = runTest {
        stored(preference(subtitleOff = true, subtitleLabel = "Fansub"))

        val match = findMatch()

        assertEquals(AllohaTrackMatch(audioId = null, applySubtitleChange = true, subtitleIndex = null), match)
    }

    @Test
    fun `label and language together win over the label alone`() = runTest {
        val tracks = subtitles + AllohaSubtitleTrack(label = "Fansub", url = "u3", language = "en")
        stored(preference(subtitleLabel = "Fansub", subtitleLanguage = "en"))

        assertEquals(3, findMatch(subtitles = tracks)?.subtitleIndex)
    }

    @Test
    fun `label alone is used when the language changed`() = runTest {
        stored(preference(subtitleLabel = "Fansub", subtitleLanguage = "uk"))

        assertEquals(1, findMatch()?.subtitleIndex)
    }

    @Test
    fun `language is used only when it points to a single track`() = runTest {
        stored(preference(subtitleLanguage = "en"))
        assertEquals(2, findMatch()?.subtitleIndex)

        stored(preference(subtitleLanguage = "ru"))
        val ambiguous = findMatch()
        assertEquals(false, ambiguous?.applySubtitleChange)
        assertNull(ambiguous?.subtitleIndex)
    }

    @Test
    fun `selections are saved separately`() = runTest {
        val handler = createHandler()

        handler.saveAudioSelection(ANIME_ID, DUBBING, PLAYER, "Japanese")
        handler.saveSubtitleSelection(ANIME_ID, DUBBING, PLAYER, "en", "English", subtitleOff = false)

        coVerify { saveAudio(ANIME_ID, DUBBING, PLAYER, "Japanese") }
        coVerify { saveSubtitle(ANIME_ID, DUBBING, PLAYER, "en", "English", false) }
    }

    private fun stored(preference: AllohaTrackPreference?) {
        coEvery { getPreference(ANIME_ID, DUBBING, PLAYER) } returns preference
    }

    private suspend fun findMatch(
        audioTracks: List<AllohaAudioTrack> = audio,
        subtitles: List<AllohaSubtitleTrack> = this.subtitles,
    ) = createHandler().findMatch(ANIME_ID, DUBBING, PLAYER, audioTracks, subtitles)

    private fun preference(
        audioLabel: String? = null,
        subtitleLanguage: String? = null,
        subtitleLabel: String? = null,
        subtitleOff: Boolean = false,
    ) = AllohaTrackPreference(
        animeId = ANIME_ID,
        dubbing = DUBBING,
        player = PLAYER,
        audioLabel = audioLabel,
        subtitleLanguage = subtitleLanguage,
        subtitleLabel = subtitleLabel,
        subtitleOff = subtitleOff,
    )

    private fun createHandler() = PlayerAllohaTrackPreferenceHandler(getPreference, saveAudio, saveSubtitle)

    private companion object {
        const val ANIME_ID = 42
        const val DUBBING = "AniLibria"
        const val PLAYER = "Alloha"
    }
}
