package su.afk.yummy.tv.data.details.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsCache
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsEntry
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.details.dto.YummyEpisodesDto
import su.afk.yummy.tv.data.details.network.MalEpisodeFlagsApi
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.network.YummyEpisodeFlagsApi
import su.afk.yummy.tv.data.details.network.YummyEpisodesApi

/**
 * Язык названий и описаний серий берётся из настройки языка API в приложении, а не из локали устройства:
 * он уходит в `?lang=` и в ключ кэша, чтобы смена языка не показывала описания на старом.
 */
class YaniAnimeRepositoryEpisodeLanguageTest : BaseUnitTest() {

    private val animeStorage: AnimeStorage = mockk(relaxed = true)
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val episodesApi: YummyEpisodesApi = mockk()
    private val documentCache: DocumentCacheStorage = mockk()
    private val cacheKey = slot<String>()

    private fun createRepository(language: YaniContentLanguage): YaniAnimeRepository {
        every { settingsStore.yaniContentLanguage } returns flowOf(language)
        coEvery { animeStorage.getDetails(ANIME_ID, language.apiCode) } returns freshDetails(language)
        coEvery { episodesApi.getEpisodes(MAL_ID, any()) } returns YummyEpisodesDto(malId = MAL_ID)
        coEvery {
            documentCache.getOrFetch<Any?>(capture(cacheKey), any(), any(), any(), any(), any())
        } coAnswers { arg<suspend () -> Any?>(5).invoke() }
        return YaniAnimeRepository(
            api = mockk<YaniAnimeApi>(relaxed = true),
            episodesApi = episodesApi,
            episodeFlagsApi = mockk<MalEpisodeFlagsApi>(relaxed = true),
            episodeFlagsBackupApi = mockk<YummyEpisodeFlagsApi>(relaxed = true),
            animeStorage = animeStorage,
            accountStorage = mockk<AccountStorage>(relaxed = true),
            settingsStore = settingsStore,
            watchProgressStore = mockk<WatchProgressStorage>(relaxed = true),
            documentCache = documentCache,
        )
    }

    private fun freshDetails(language: YaniContentLanguage) = AnimeDetailsCache(
        entry = AnimeDetailsEntry(
            animeId = ANIME_ID,
            language = language.apiCode,
            animeUrl = "url",
            title = "title",
            description = "description",
            malId = MAL_ID,
            cachedAt = System.currentTimeMillis(),
        ),
        otherTitles = emptyList(),
        genres = emptyList(),
        creators = emptyList(),
        studios = emptyList(),
        viewingOrder = emptyList(),
        screenshots = emptyList(),
    )

    @Test
    fun `episode language follows the API language setting`() = runBlocking {
        YaniContentLanguage.entries.forEach { language ->
            val repository = createRepository(language)

            repository.getAnimeEpisodeInfo(ANIME_ID)

            coVerify { episodesApi.getEpisodes(MAL_ID, language.apiCode) }
            assertTrue(cacheKey.captured.endsWith(":$MAL_ID:${language.apiCode}"))
        }
    }

    private companion object {
        const val ANIME_ID = 1512
        const val MAL_ID = 52991
    }
}
