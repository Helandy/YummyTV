package su.afk.yummy.tv.data.details.thumbnail

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.model.anime.AnimeTmdbImages
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceModePreferences
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeTmdbImagesUseCase

class TmdbEpisodePreviewsTest : BaseUnitTest() {

    private val repository: AnimeRepository = mockk()
    private val clock: AppClock = mockk { every { nowMillis() } returns 0L }
    private val interfaceMode: AppInterfaceModePreferences = mockk()

    private fun kotlinx.coroutines.test.TestScope.previews(mode: AppInterfaceMode?): TmdbEpisodePreviews {
        every { interfaceMode.selectedMode } returns mode
        return TmdbEpisodePreviews(GetAnimeTmdbImagesUseCase(repository), interfaceMode, backgroundScope, clock)
    }

    private fun withPreviews(vararg pairs: Pair<String, String>) {
        coEvery { repository.getAnimeTmdbImages(ANIME_ID) } returns AnimeTmdbImages(episodePreviewUrls = mapOf(*pairs))
    }

    @Test
    fun `mobile gets a bigger frame`() = runTest {
        withPreviews("1" to W300)

        assertEquals(W300.replace("/w300/", "/w780/"), previews(AppInterfaceMode.MOBILE).previewUrl(ANIME_ID, "1"))
    }

    @Test
    fun `tv gets the biggest frame`() = runTest {
        withPreviews("1" to W300)

        assertEquals(W300.replace("/w300/", "/w1280/"), previews(AppInterfaceMode.TV).previewUrl(ANIME_ID, "01"))
    }

    @Test
    fun `unknown episode and unknown title give null`() = runTest {
        withPreviews("1" to W300)
        val previews = previews(AppInterfaceMode.MOBILE)

        assertNull(previews.previewUrl(ANIME_ID, "2"))
        assertNull(previews.previewUrl(0, "1"))
        assertNull(previews.previewUrl(ANIME_ID, " "))
    }

    @Test
    fun `title is loaded once for many episodes`() = runTest {
        withPreviews("1" to W300, "2" to W300)
        val previews = previews(AppInterfaceMode.MOBILE)

        previews.previewUrl(ANIME_ID, "1")
        previews.previewUrl(ANIME_ID, "2")

        coVerify(exactly = 1) { repository.getAnimeTmdbImages(ANIME_ID) }
    }

    private companion object {
        const val ANIME_ID = 7
        const val W300 = "https://image.tmdb.org/t/p/w300/a.jpg"
    }
}
