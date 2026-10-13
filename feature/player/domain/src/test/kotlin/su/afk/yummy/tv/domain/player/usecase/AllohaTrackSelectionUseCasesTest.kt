package su.afk.yummy.tv.domain.player.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.player.model.AllohaTrackPreference
import su.afk.yummy.tv.domain.player.repository.AllohaTrackPreferenceRepository

class AllohaTrackSelectionUseCasesTest : BaseUnitTest() {

    private val repository: AllohaTrackPreferenceRepository = mockk(relaxed = true)

    private val stored = AllohaTrackPreference(
        animeId = ANIME_ID,
        dubbing = DUBBING,
        player = PLAYER,
        audioLabel = "Японский",
        subtitleLanguage = "ru",
        subtitleLabel = "Fansub",
        subtitleOff = false,
    )

    @Test
    fun `audio choice keeps the stored subtitles`() = runTest {
        coEvery { repository.get(ANIME_ID, DUBBING, PLAYER) } returns stored

        SaveAllohaAudioSelectionUseCase(repository)(ANIME_ID, DUBBING, PLAYER, "Русский")

        coVerify { repository.save(stored.copy(audioLabel = "Русский")) }
    }

    @Test
    fun `subtitle choice keeps the stored audio`() = runTest {
        coEvery { repository.get(ANIME_ID, DUBBING, PLAYER) } returns stored

        SaveAllohaSubtitleSelectionUseCase(repository)(ANIME_ID, DUBBING, PLAYER, null, null, subtitleOff = true)

        coVerify {
            repository.save(stored.copy(subtitleLanguage = null, subtitleLabel = null, subtitleOff = true))
        }
    }

    @Test
    fun `first choice starts a fresh record`() = runTest {
        coEvery { repository.get(any(), any(), any()) } returns null

        SaveAllohaAudioSelectionUseCase(repository)(ANIME_ID, DUBBING, PLAYER, "Русский")

        coVerify {
            repository.save(AllohaTrackPreference(ANIME_ID, DUBBING, PLAYER, "Русский", null, null, subtitleOff = false))
        }
    }

    @Test
    fun `incomplete key is ignored`() = runTest {
        SaveAllohaAudioSelectionUseCase(repository)(0, DUBBING, PLAYER, "Русский")
        SaveAllohaSubtitleSelectionUseCase(repository)(ANIME_ID, "", PLAYER, "en", "English", subtitleOff = false)
        SaveAllohaSubtitleSelectionUseCase(repository)(ANIME_ID, DUBBING, " ", "en", "English", subtitleOff = false)

        coVerify(exactly = 0) { repository.save(any()) }
    }

    private companion object {
        const val ANIME_ID = 42
        const val DUBBING = "AniLibria"
        const val PLAYER = "Alloha"
    }
}
