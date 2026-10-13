package su.afk.yummy.tv.feature.account.account.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.usecase.GetNotificationCountsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetProfileNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserProfileSummaryUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserStatsUseCase
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

class AccountHubHandlerTest : BaseUnitTest() {

    private val settingsStore: YaniAccountSettingsStore = mockk(relaxed = true)
    private val getProfileSummary: GetUserProfileSummaryUseCase = mockk()
    private val getUserStats: GetUserStatsUseCase = mockk()
    private val getNotifications: GetProfileNotificationsUseCase = mockk()
    private val getNotificationCounts: GetNotificationCountsUseCase = mockk()

    @Test
    fun `hub parts fail independently`() = runTest {
        val summary: UserProfileSummary = mockk()
        coEvery { getProfileSummary(USER_ID) } returns summary
        coEvery { getUserStats(USER_ID) } throws IllegalStateException("stats")
        coEvery { getNotifications(limit = 20) } returns emptyList()
        coEvery { getNotificationCounts() } returns emptyList()

        val result = createHandler().loadHub(USER_ID)

        assertEquals(summary, result.profileSummary)
        assertNull(result.profileSummaryError)
        assertNull(result.stats)
        assertEquals(AccountUiError.LOAD_PROFILE_STATISTICS_FAILED, result.statsError)
        assertEquals(AccountNotificationsLoadResult.Success(emptyList(), emptyList()), result.notifications)
    }

    @Test
    fun `loaded notifications update the unread badge without private messages`() = runTest {
        coEvery { getNotifications(limit = 20) } returns emptyList()
        coEvery { getNotificationCounts() } returns listOf(
            NotificationCount(type = "episode", count = 3),
            NotificationCount(type = "comment", count = 2),
            NotificationCount(type = "message", count = 9),
        )

        createHandler().loadNotifications()

        coVerify { settingsStore.setYaniUnreadNotificationsCount(5) }
    }

    @Test
    fun `failed notifications keep the badge and report the error`() = runTest {
        coEvery { getNotifications(limit = 20) } throws IllegalStateException("network")

        val result = createHandler().loadNotifications()

        assertEquals(AccountNotificationsLoadResult.Failure(AccountUiError.LOAD_NOTIFICATIONS_FAILED), result)
        coVerify(exactly = 0) { settingsStore.setYaniUnreadNotificationsCount(any()) }
    }

    private fun createHandler() = AccountHubHandler(
        settingsStore = settingsStore,
        getUserProfileSummary = getProfileSummary,
        getUserStats = getUserStats,
        getNotifications = getNotifications,
        getNotificationCounts = getNotificationCounts,
    )

    private companion object {
        const val USER_ID = 5
    }
}
