package su.afk.yummy.tv.data.details.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.network.yani.MirrorTrace
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.anime.ANIME_DETAIL_NAMED_KIND_GENRE
import su.afk.yummy.tv.core.storage.anime.AnimeDetailNamedEntry
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsCache
import su.afk.yummy.tv.core.storage.anime.AnimeDetailsEntry
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.details.dto.YaniAnimeDetailsDto
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.network.YummyEpisodeFlagsApi
import su.afk.yummy.tv.data.details.network.YummyEpisodesApi

/**
 * Пока yani недоступен, ответ резервной копии не должен затирать то, что уже лежит в кэше: минимальная карточка копии не
 * заменяет полную, а оценка пользователя из копии (в ней нет поля `user`) не пишется.
 */
class YaniAnimeRepositoryMirrorTest : BaseUnitTest() {

    private val api: YaniAnimeApi = mockk()
    private val animeStorage: AnimeStorage = mockk(relaxed = true)
    private val accountStorage: AccountStorage = mockk(relaxed = true)
    private val settingsStore: YaniAccountSettingsStore = mockk()

    @Before
    fun setUp() {
        every { settingsStore.yaniContentLanguage } returns flowOf(YaniContentLanguage.RUSSIAN)
        every { settingsStore.yaniUserId } returns flowOf(USER_ID)
    }

    private fun repository() = YaniAnimeRepository(
        api = api,
        episodesApi = mockk<YummyEpisodesApi>(relaxed = true),
        episodeFlagsBackupApi = mockk<YummyEpisodeFlagsApi>(relaxed = true),
        animeStorage = animeStorage,
        accountStorage = accountStorage,
        settingsStore = settingsStore,
        watchProgressStore = mockk<WatchProgressStorage>(relaxed = true),
        documentCache = mockk<DocumentCacheStorage>(relaxed = true),
    )

    private fun storedFullDetails() = AnimeDetailsCache(
        entry = AnimeDetailsEntry(animeId = ANIME_ID, language = "ru", animeUrl = "u", title = "Клинок", description = "Полное описание", cachedAt = 0),
        otherTitles = emptyList(),
        genres = listOf(AnimeDetailNamedEntry(ANIME_ID, "ru", ANIME_DETAIL_NAMED_KIND_GENRE, 0, 5, "Экшен", null)),
        creators = emptyList(), studios = emptyList(), viewingOrder = emptyList(), screenshots = emptyList(),
    )

    private fun minimalDto() = Json { ignoreUnknownKeys = true }.decodeFromString<YaniAnimeDetailsDto>(
        """{"response":{"anime_id":$ANIME_ID,"title":"Клинок","description":"","remote_ids":{"myanimelist_id":38000}}}""",
    )

    @Test
    fun `minimal copy details do not replace stored full details and no rating is written`() = runBlocking {
        coEvery { animeStorage.getDetails(ANIME_ID, any()) } returns storedFullDetails()
        coEvery { api.getAnimeDetails(ANIME_ID) } coAnswers {
            currentCoroutineContext()[MirrorTrace]?.fromMirror = true
            minimalDto()
        }

        val details = repository().getAnimeDetails(ANIME_ID)

        assertEquals("Полное описание", details.description)
        coVerify(exactly = 0) { animeStorage.saveDetails(any()) }
        coVerify(exactly = 0) { accountStorage.saveUserRating(any()) }
    }

    @Test
    fun `minimal copy details fill an empty cache but expire soon`() = runBlocking {
        coEvery { animeStorage.getDetails(ANIME_ID, any()) } returns null
        coEvery { api.getAnimeDetails(ANIME_ID) } coAnswers {
            currentCoroutineContext()[MirrorTrace]?.fromMirror = true
            minimalDto()
        }
        val before = System.currentTimeMillis()

        val details = repository().getAnimeDetails(ANIME_ID)

        assertEquals(38000, details.malId)
        coVerify(exactly = 1) {
            animeStorage.saveDetails(
                match { it.entry.cachedAt < before - 23 * 60 * 60 * 1000L },
            )
        }
        coVerify(exactly = 0) { accountStorage.saveUserRating(any()) }
    }

    private companion object {
        const val ANIME_ID = 42
        const val USER_ID = 7
    }
}
