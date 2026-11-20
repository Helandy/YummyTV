package su.afk.yummy.tv.feature.account.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.account.model.NotificationCount
import su.afk.yummy.tv.domain.account.model.ProfileNotification
import su.afk.yummy.tv.feature.account.notifications.NotificationsState
import su.afk.yummy.tv.feature.account.presentation.R

/** ТВ-вкладка уведомлений на подставном состоянии NotificationsViewModel: тексты и события. */
class AccountTvNotificationsTabTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val accountEvents = mutableListOf<AccountState.Event>()
    private val notificationsEvents = mutableListOf<NotificationsState.Event>()

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun notification(id: Int, viewed: Boolean = false, slug: String? = "slug$id") = ProfileNotification(
        id = id,
        dateSeconds = 0L,
        title = "Серия $id вышла",
        text = "",
        clickUri = "",
        type = "anime",
        subType = "",
        viewed = viewed,
        objectId = null,
        animeSlug = slug,
        isNewEpisode = slug != null,
    )

    private fun signedIn() = AccountState.State(
        isSessionResolved = true,
        isSignedIn = true,
        userId = 12,
        nickname = "ТестовыйАккаунт",
        selectedTab = AccountState.AccountTab.NOTIFICATIONS,
        unreadNotificationCount = 2,
    )

    private fun setScreen(
        notifications: NotificationsState.State,
        account: AccountState.State = signedIn(),
    ) {
        composeRule.setContent {
            ScreenPreviewTheme {
                // ТВ-раскладка рассчитана на широкий экран; на телефонном эмуляторе без этого
                // кнопки внутри строк сжимаются до нулевой ширины.
                Box(Modifier.requiredSize(960.dp, 540.dp)) {
                    AccountTvScreen(
                        state = account,
                        notificationsState = notifications,
                        effect = emptyFlow(),
                        onEvent = { accountEvents += it },
                        onNotificationsEvent = { notificationsEvents += it },
                    )
                }
            }
        }
    }

    private fun withNotifications() = NotificationsState.State(
        notifications = persistentListOf(notification(1), notification(2, viewed = true)),
        notificationCounts = persistentListOf(NotificationCount("anime", 1)),
    )

    @Test
    fun screenShownGoesToBothViewModels() {
        setScreen(withNotifications())
        composeRule.waitForIdle()

        assertTrue(AccountState.Event.ScreenShown in accountEvents)
        assertTrue(NotificationsState.Event.ScreenShown in notificationsEvents)
    }

    @Test
    fun showsNotificationsFromTheSeparateState() {
        setScreen(withNotifications())

        composeRule.onNodeWithText("Серия 1 вышла").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.account_my_subscriptions)).assertIsDisplayed()
    }

    @Test
    fun emptyListShowsTheEmptyText() {
        setScreen(NotificationsState.State())

        composeRule.onNodeWithText(string(R.string.account_notifications_empty)).assertIsDisplayed()
    }

    @Test
    fun loadingWithoutDataHidesTheEmptyText() {
        setScreen(NotificationsState.State(isNotificationsLoading = true))

        composeRule.onNodeWithText(string(R.string.account_notifications_empty)).assertDoesNotExist()
    }

    @Test
    fun mySubscriptionsSendsTheEventToTheNotificationsViewModel() {
        setScreen(withNotifications())

        composeRule.onNodeWithText(string(R.string.account_my_subscriptions)).performClick()

        assertTrue(NotificationsState.Event.MySubscriptionsSelected in notificationsEvents)
        assertTrue(accountEvents.none { it !is AccountState.Event.ScreenShown })
    }

    @Test
    fun clickingAnExpandableNotificationTogglesIt() {
        setScreen(
            NotificationsState.State(
                notifications = persistentListOf(notification(1).copy(text = "Полный текст")),
            ),
        )

        composeRule.onNodeWithText("Серия 1 вышла").performClick()

        assertTrue("events=$notificationsEvents", NotificationsState.Event.NotificationExpandToggled(1) in notificationsEvents)
    }

    @Test
    fun openButtonOfAnExpandedNotificationSendsNotificationSelected() {
        setScreen(
            NotificationsState.State(
                notifications = persistentListOf(notification(1).copy(text = "Полный текст")),
                expandedNotificationIds = persistentSetOf(1),
            ),
        )

        composeRule.onNodeWithText(string(R.string.account_open)).performScrollTo().performClick()

        assertTrue(
            "events=$notificationsEvents",
            NotificationsState.Event.NotificationSelected(1) in notificationsEvents,
        )
    }

    @Test
    fun markAllReadIsOfferedOnlyWithUnreadAndSendsAfterConfirm() {
        setScreen(withNotifications())

        composeRule.onNodeWithText(string(R.string.account_mark_all_read)).performClick()
        composeRule.onNodeWithText(string(R.string.account_mark_all_read_message)).assertIsDisplayed()
        assertTrue(NotificationsState.Event.AllNotificationsReadSelected !in notificationsEvents)

        // Кнопка подтверждения в диалоге идёт вторым узлом с той же подписью.
        composeRule.onAllNodesWithText(string(R.string.account_mark_all_read)).onLast().performClick()

        assertTrue(NotificationsState.Event.AllNotificationsReadSelected in notificationsEvents)
    }

    @Test
    fun noUnreadMeansNoMarkAllReadAction() {
        setScreen(
            NotificationsState.State(notifications = persistentListOf(notification(2, viewed = true))),
        )

        composeRule.onNodeWithText(string(R.string.account_mark_all_read)).assertDoesNotExist()
    }
}
