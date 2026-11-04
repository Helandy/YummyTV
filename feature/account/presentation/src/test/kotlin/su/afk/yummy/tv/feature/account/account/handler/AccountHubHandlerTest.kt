package su.afk.yummy.tv.feature.account.account.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.usecase.GetCachedProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetCachedUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetCachedUserStatsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase

class AccountHubHandlerTest : BaseUnitTest() {

    private val settingsStore: YaniAccountSettingsStore = mockk(relaxed = true)
    private val getProfileSummary: GetUserProfileSummaryUseCase = mockk()
    private val getUserStats: GetUserStatsUseCase = mockk()
    private val getNotifications: GetProfileNotificationsUseCase = mockk()
    private val getNotificationCounts: GetNotificationCountsUseCase = mockk()
    private val getCachedSummary: GetCachedUserProfileSummaryUseCase = mockk()
    private val getCachedStats: GetCachedUserStatsUseCase = mockk()
    private val getCachedNotifications: GetCachedProfileNotificationsUseCase = mockk()

    @Test
    fun `hub parts fail independently`() = runTest {
        val summary: UserProfileSummary = mockk()
        coEvery { getProfileSummary(USER_ID) } returns summary
        coEvery { getUserStats(USER_ID) } throws IllegalStateException("stats")

        val handler = createHandler()

        assertEquals(summary, handler.loadProfileSummary(USER_ID).getOrNull())
        assertTrue(handler.loadStats(USER_ID).isFailure)
    }

    @Test
    fun `next notifications page is requested by offset`() = runTest {
        coEvery { getNotifications(limit = 20, offset = 40) } returns emptyList()

        createHandler().loadNotifications(offset = 40)

        coVerify(exactly = 1) { getNotifications(limit = 20, offset = 40) }
    }

    @Test
    fun `cached hub returns what is stored and ignores read failures`() = runTest {
        val summary: UserProfileSummary = mockk()
        coEvery { getCachedSummary(USER_ID) } returns summary
        coEvery { getCachedStats(USER_ID) } throws IllegalStateException("room")
        coEvery { getCachedNotifications(20) } returns null

        val cached = createHandler().loadCached(USER_ID)

        assertEquals(summary, cached.profileSummary)
        assertNull(cached.stats)
        assertNull(cached.notifications)
    }

    @Test
    fun `loaded notification counts update the unread badge without private messages`() = runTest {
        coEvery { getNotificationCounts() } returns listOf(
            NotificationCount(type = "episode", count = 3),
            NotificationCount(type = "comment", count = 2),
            NotificationCount(type = "message", count = 9),
        )

        createHandler().loadNotificationCounts()

        coVerify { settingsStore.setYaniUnreadNotificationsCount(5) }
    }

    @Test
    fun `failed notification counts keep the badge`() = runTest {
        coEvery { getNotificationCounts() } throws IllegalStateException("network")

        val result = createHandler().loadNotificationCounts()

        assertTrue(result.isFailure)
        coVerify(exactly = 0) { settingsStore.setYaniUnreadNotificationsCount(any()) }
    }

    private fun createHandler() = AccountHubHandler(
        settingsStore = settingsStore,
        getUserProfileSummary = getProfileSummary,
        getUserStats = getUserStats,
        getNotifications = getNotifications,
        getNotificationCounts = getNotificationCounts,
        getCachedUserProfileSummary = getCachedSummary,
        getCachedUserStats = getCachedStats,
        getCachedNotifications = getCachedNotifications,
    )

    private companion object {
        const val USER_ID = 5
    }
}
