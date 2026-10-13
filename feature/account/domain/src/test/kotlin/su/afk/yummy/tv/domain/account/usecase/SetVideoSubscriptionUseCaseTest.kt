package su.afk.yummy.tv.domain.account.usecase

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.AccountMutationAction
import su.afk.yummy.tv.domain.account.model.AccountMutationErrorEvent
import su.afk.yummy.tv.domain.account.repository.AccountMutationErrorRepository
import su.afk.yummy.tv.domain.account.repository.VideoSubscriptionRepository

class SetVideoSubscriptionUseCaseTest : BaseUnitTest() {

    private val repository: VideoSubscriptionRepository = mockk()
    private val notifier: AccountMutationErrorRepository = mockk(relaxed = true)

    @Test
    fun `successful change is silent`() = runTest {
        coEvery { repository.setSubscribed(VIDEO_ID, true) } returns true

        SetVideoSubscriptionUseCase(repository, notifier)(VIDEO_ID, subscribed = true)

        coVerify(exactly = 0) { notifier.notify(any()) }
    }

    @Test
    fun `refused change is reported with its action`() = runTest {
        coEvery { repository.setSubscribed(VIDEO_ID, false) } returns false

        assertFalse(SetVideoSubscriptionUseCase(repository, notifier)(VIDEO_ID, subscribed = false))

        coVerify { notifier.notify(AccountMutationErrorEvent(AccountMutationAction.REMOVE_VIDEO_SUBSCRIPTION, null)) }
    }

    @Test
    fun `crashed change is reported and rethrown`() = runTest {
        coEvery { repository.setSubscribed(VIDEO_ID, true) } throws IllegalStateException("500")

        val error = runCatching { SetVideoSubscriptionUseCase(repository, notifier)(VIDEO_ID, subscribed = true) }.exceptionOrNull()

        assertEquals("500", error?.message)
        coVerify { notifier.notify(AccountMutationErrorEvent(AccountMutationAction.SET_VIDEO_SUBSCRIPTION, "500")) }
    }

    private companion object {
        const val VIDEO_ID = 7
    }
}
