package su.afk.yummy.tv.domain.account.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.repository.ProfileNotificationsRepository

class GetNotificationCountsUseCaseTest : BaseUnitTest() {

    private val repository: ProfileNotificationsRepository = mockk()

    @Test
    fun `counts come from a single counters request`() = runTest {
        val expected = listOf(NotificationCount("episode", 98), NotificationCount("comment", 2))
        coEvery { repository.getNotificationCounts() } returns expected

        val counts = GetNotificationCountsUseCase(repository)()

        assertEquals(expected, counts)
        coVerify(exactly = 1) { repository.getNotificationCounts() }
        coVerify(exactly = 0) { repository.getNotifications(any(), any()) }
    }
}
