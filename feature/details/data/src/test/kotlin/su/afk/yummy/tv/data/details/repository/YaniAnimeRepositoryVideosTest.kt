package su.afk.yummy.tv.data.details.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.anime.AnimeStorage
import su.afk.yummy.tv.core.storage.document.DocumentCacheStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.details.dto.YaniAnimeVideosDto
import su.afk.yummy.tv.data.details.network.MalEpisodeFlagsApi
import su.afk.yummy.tv.data.details.network.YaniAnimeApi
import su.afk.yummy.tv.data.details.network.YummyEpisodesApi
import java.util.concurrent.atomic.AtomicInteger

/**
 * Загрузка видео живёт в общем запросе на тайтл и пользователя: гонка вызывающих не должна слать
 * мегабайтный `/videos` несколько раз. Репозиторий ходит в реальные `Dispatchers.IO`, поэтому тесты —
 * на `runBlocking` с воротами, а не на виртуальном времени.
 */
class YaniAnimeRepositoryVideosTest : BaseUnitTest() {

    private val api: YaniAnimeApi = mockk()
    private val animeStorage: AnimeStorage = mockk(relaxed = true)
    private val settingsStore: YaniAccountSettingsStore = mockk()

    private val apiCalls = AtomicInteger()
    private val userIdReads = AtomicInteger()
    private var userIdOf: (read: Int) -> Int = { USER_ID }
    private val gate = CompletableDeferred<Unit>()

    @Before
    fun setUp() {
        every { settingsStore.yaniContentLanguage } returns flowOf(YaniContentLanguage.RUSSIAN)
        every { settingsStore.yaniUserId } answers {
            flow { emit(userIdOf(userIdReads.incrementAndGet())) }
        }
        coEvery { animeStorage.getVideos(any(), any()) } returns null
        coEvery { api.getAnimeVideos(ANIME_ID) } coAnswers {
            apiCalls.incrementAndGet()
            gate.await()
            YaniAnimeVideosDto()
        }
    }

    private fun createRepository() = YaniAnimeRepository(
        api = api,
        episodesApi = mockk<YummyEpisodesApi>(relaxed = true),
        episodeFlagsApi = mockk<MalEpisodeFlagsApi>(relaxed = true),
        animeStorage = animeStorage,
        accountStorage = mockk<AccountStorage>(relaxed = true),
        settingsStore = settingsStore,
        watchProgressStore = mockk<WatchProgressStorage>(relaxed = true),
        documentCache = mockk<DocumentCacheStorage>(relaxed = true),
    )

    @Test
    fun `concurrent callers share one videos request`() = runBlocking {
        val repository = createRepository()

        val first = async(Dispatchers.Default) { repository.getAnimeVideos(ANIME_ID) }
        val second = async(Dispatchers.Default) { repository.refreshAnimeVideos(ANIME_ID) }
        awaitUntil { userIdReads.get() == 2 }
        delay(SETTLE_MS)
        gate.complete(Unit)
        first.await()
        second.await()

        coVerify(exactly = 1) { api.getAnimeVideos(ANIME_ID) }
    }

    @Test
    fun `callers of different users do not share a request`() = runBlocking {
        userIdOf = { read -> read }
        val repository = createRepository()

        val first = async(Dispatchers.Default) { repository.getAnimeVideos(ANIME_ID) }
        val second = async(Dispatchers.Default) { repository.getAnimeVideos(ANIME_ID) }
        awaitUntil { apiCalls.get() == 2 }
        gate.complete(Unit)
        first.await()
        second.await()

        coVerify(exactly = 2) { api.getAnimeVideos(ANIME_ID) }
    }

    @Test
    fun `a finished request is not reused by a later call`() = runBlocking {
        gate.complete(Unit)
        val repository = createRepository()

        repository.getAnimeVideos(ANIME_ID)
        repository.getAnimeVideos(ANIME_ID)

        coVerify(exactly = 2) { api.getAnimeVideos(ANIME_ID) }
    }

    @Test
    fun `cancelling one waiter does not cancel the shared request for the others`() = runBlocking {
        val repository = createRepository()

        val cancelled = async(Dispatchers.Default) { repository.getAnimeVideos(ANIME_ID) }
        val survivor = async(Dispatchers.Default) { repository.getAnimeVideos(ANIME_ID) }
        awaitUntil { userIdReads.get() == 2 }
        delay(SETTLE_MS)
        cancelled.cancelAndJoin()
        gate.complete(Unit)

        withTimeout(TIMEOUT_MS) { survivor.await() }
        coVerify(exactly = 1) { api.getAnimeVideos(ANIME_ID) }
    }

    private suspend fun awaitUntil(condition: () -> Boolean) = withTimeout(TIMEOUT_MS) {
        while (!condition()) delay(POLL_MS)
    }

    private companion object {
        const val ANIME_ID = 1512
        const val USER_ID = 7
        const val SETTLE_MS = 100L
        const val POLL_MS = 5L
        const val TIMEOUT_MS = 5_000L
    }
}
