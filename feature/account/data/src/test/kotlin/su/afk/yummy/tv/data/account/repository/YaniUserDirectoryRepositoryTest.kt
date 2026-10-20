package su.afk.yummy.tv.data.account.repository

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.account.AccountStorage
import su.afk.yummy.tv.core.storage.account.AccountUserProfileSummaryCache
import su.afk.yummy.tv.core.storage.account.AccountUserProfileSummaryCacheEntry
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.data.account.network.YaniAccountApi

class YaniUserDirectoryRepositoryTest : BaseUnitTest() {
    private val api: YaniAccountApi = mockk()
    private val storage: AccountStorage = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()

    @Before
    fun setUp() {
        every { settingsStore.yaniContentLanguage } returns flowOf(
            YaniContentLanguage.entries.first(),
        )
    }

    private fun createRepository() = YaniUserDirectoryRepository(api, storage, settingsStore)

    private fun cached(cachedAt: Long) = AccountUserProfileSummaryCache(
        entry = AccountUserProfileSummaryCacheEntry(
            userId = 42, language = "ru", cachedAt = cachedAt, nickname = "Sandy",
            registerDateSeconds = 0, birthDateSeconds = 0, sex = 0, about = "", daysOnline = 0,
            watchingCount = 0, plannedCount = 0, completedCount = 0, droppedCount = 0,
            postponedCount = 0, favoriteCount = 0, friendsCount = 0, reviewsCount = 0,
            commentsCount = 0, postsCount = 0, collectionsCount = 0,
        ),
        watchTypes = emptyList(),
        watchHistory = emptyList(),
    )

    @Test
    fun `fresh cached profile is served by nickname without network`() = runBlocking {
        coEvery { storage.getUserProfileSummaryByNickname("sandy", any()) } returns
            cached(System.currentTimeMillis())

        val profile = createRepository().getProfileByNickname("sandy")

        assertEquals(42, profile.userId)
        coVerify(exactly = 0) { api.getUserProfileByNickname(any()) }
    }

    @Test
    fun `stale cached profile is served when network fails`() = runBlocking {
        coEvery { storage.getUserProfileSummaryByNickname("sandy", any()) } returns
            cached(System.currentTimeMillis() - 2 * ACCOUNT_MEDIUM_TTL_MS)
        coEvery { api.getUserProfileByNickname("sandy") } throws IOException("offline")

        val profile = createRepository().getProfileByNickname("sandy")

        assertEquals(42, profile.userId)
    }
}
