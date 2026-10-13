package su.afk.yummy.tv.domain.account.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class GetNotificationCountsUseCaseTest : BaseUnitTest() {

    private val repository: ProfileNotificationsRepository = mockk()

    @Test
    fun `unread notifications are counted by type across pages`() = runTest {
        coEvery { repository.getNotifications(100, 0) } returns
            List(98) { notification(it, "episode", viewed = false) } + notification(98, "comment", false) + notification(99, "episode", true)
        coEvery { repository.getNotifications(100, 100) } returns listOf(notification(100, "comment", viewed = false))

        val counts = GetNotificationCountsUseCase(repository)()

        assertEquals(setOf(NotificationCount("episode", 98), NotificationCount("comment", 2)), counts.toSet())
    }

    @Test
    fun `reading stops at a short page`() = runTest {
        coEvery { repository.getNotifications(100, 0) } returns listOf(notification(1, "episode", viewed = false))

        GetNotificationCountsUseCase(repository)()

        coVerify(exactly = 1) { repository.getNotifications(any(), any()) }
    }

    @Test
    fun `reading is capped at ten pages`() = runTest {
        coEvery { repository.getNotifications(100, any()) } returns List(100) { notification(it, "episode", viewed = true) }

        GetNotificationCountsUseCase(repository)()

        coVerify(exactly = 10) { repository.getNotifications(any(), any()) }
    }

    private fun notification(id: Int, type: String, viewed: Boolean) = ProfileNotification(
        id = id,
        dateSeconds = 0L,
        title = "",
        text = "",
        clickUri = "",
        type = type,
        subType = "",
        viewed = viewed,
        objectId = null,
    )
}
