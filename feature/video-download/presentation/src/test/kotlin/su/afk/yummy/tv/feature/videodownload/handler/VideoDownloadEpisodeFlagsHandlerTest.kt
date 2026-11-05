package su.afk.yummy.tv.feature.videodownload.handler

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.anime.EpisodeFlags
import su.afk.yummy.tv.core.preferences.settings.AppearanceSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.ObserveAnimeEpisodeFlagsUseCase

class VideoDownloadEpisodeFlagsHandlerTest : BaseUnitTest() {
    private val animeRepository: AnimeRepository = mockk()
    private val appearanceSettings: AppearanceSettingsStore = mockk()

    private fun handler(enabled: Boolean): VideoDownloadEpisodeFlagsHandler {
        every { appearanceSettings.showEpisodeFlags } returns flowOf(enabled)
        every { animeRepository.observeAnimeEpisodeFlags(7) } returns
            flowOf(mapOf("01" to EpisodeFlags(filler = true, recap = false)))
        return VideoDownloadEpisodeFlagsHandler(ObserveAnimeEpisodeFlagsUseCase(animeRepository), appearanceSettings)
    }

    @Test
    fun `flags are keyed by anime id and normalized episode number`() = runTest {
        val flags = handler(enabled = true).observe(flowOf(setOf(7))).first { it.isNotEmpty() && it.getValue(7).isNotEmpty() }

        assertEquals(EpisodeFlags(filler = true, recap = false), flags.getValue(7)["1"])
    }

    @Test
    fun `no flags when the setting is off`() = runTest {
        val flags = handler(enabled = false).observe(flowOf(setOf(7))).first()

        assertTrue(flags.isEmpty())
    }
}
