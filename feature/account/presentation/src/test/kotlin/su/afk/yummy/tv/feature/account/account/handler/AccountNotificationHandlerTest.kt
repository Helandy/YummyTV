package su.afk.yummy.tv.feature.account.account.handler

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.usecase.DeleteAllNotificationsUseCase
import su.afk.yummy.tv.domain.account.usecase.DeleteNotificationUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkAllNotificationsReadUseCase
import su.afk.yummy.tv.domain.account.usecase.MarkNotificationReadUseCase
import su.afk.yummy.tv.domain.account.usecase.ResolveNotificationAnimeIdUseCase
import su.afk.yummy.tv.feature.account.account.model.AccountUiError

class AccountNotificationHandlerTest : BaseUnitTest() {

    private val settingsStore: YaniAccountSettingsStore = mockk(relaxed = true)
    private val resolveAnimeId: ResolveNotificationAnimeIdUseCase = mockk()
    private val markRead: MarkNotificationReadUseCase = mockk()
    private val markAllRead: MarkAllNotificationsReadUseCase = mockk()
    private val delete: DeleteNotificationUseCase = mockk()
    private val deleteAll: DeleteAllNotificationsUseCase = mockk()

    @Test
    fun `notification opens its title when the slug resolves`() = runTest {
        val handler = createHandler()

        coEvery { resolveAnimeId("frieren") } returns 42
        assertEquals(AccountOpenNotificationResult.Navigate(42), handler.resolveAnimeId("frieren"))

        coEvery { resolveAnimeId("unknown") } returns null
        assertEquals(AccountOpenNotificationResult.Failure, handler.resolveAnimeId("unknown"))

        coEvery { resolveAnimeId("broken") } throws IllegalStateException("network")
        assertEquals(AccountOpenNotificationResult.Failure, handler.resolveAnimeId("broken"))
    }

    @Test
    fun `clearing everything zeroes the badge only when the server agreed`() = runTest {
        val handler = createHandler()
        coEvery { markAllRead() } returns true
        coEvery { deleteAll() } returns false

        assertTrue(handler.markAllNotificationsRead().getOrThrow())
        handler.deleteAllNotifications()

        coVerify(exactly = 1) { settingsStore.setYaniUnreadNotificationsCount(0) }
    }

    @Test
    fun `mutation outcomes distinguish success, no change and failure`() = runTest {
        coEvery { markRead(1) } returns true
        coEvery { delete(1) } returns false
        coEvery { deleteAll() } throws IllegalStateException("500")
        val mutations = AccountNotificationMutationHandler(createHandler())

        assertEquals(AccountNotificationMutationOutcome.Success, mutations.markNotificationRead(1))
        assertEquals(AccountNotificationMutationOutcome.Unchanged, mutations.deleteNotification(1))
        assertEquals(
            AccountNotificationMutationOutcome.Failure(AccountUiError.UPDATE_NOTIFICATIONS_FAILED),
            mutations.deleteAllNotifications(),
        )
    }

    @Test
    fun `single notification failure uses its own error`() = runTest {
        coEvery { markRead(1) } throws IllegalStateException("500")

        assertEquals(
            AccountNotificationMutationOutcome.Failure(AccountUiError.UPDATE_NOTIFICATION_FAILED),
            AccountNotificationMutationHandler(createHandler()).markNotificationRead(1),
        )
    }

    private fun createHandler() = AccountNotificationHandler(
        settingsStore = settingsStore,
        resolveNotificationAnimeId = resolveAnimeId,
        markNotificationReadUseCase = markRead,
        markAllNotificationsReadUseCase = markAllRead,
        deleteNotificationUseCase = delete,
        deleteAllNotificationsUseCase = deleteAll,
    )
}
