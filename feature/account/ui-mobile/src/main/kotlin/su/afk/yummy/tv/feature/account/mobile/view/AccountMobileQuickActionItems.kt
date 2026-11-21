package su.afk.yummy.tv.feature.account.mobile.view

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.account.model.AccountMobileQuickAction
import su.afk.yummy.tv.feature.account.presentation.R

/**
 * Быстрые действия верхнего ряда экрана аккаунта: FAQ, настройки, скачанное, а для вошедшего
 * пользователя ещё и уведомления. [lastActionFocusRequester] получает последняя кнопка ряда,
 * чтобы фокус со вкладок снизу поднимался на неё.
 */
@Composable
internal fun accountMobileQuickActions(
    state: AccountState.State,
    onEvent: (AccountState.Event) -> Unit,
    lastActionFocusRequester: FocusRequester?,
): List<AccountMobileQuickAction> {
    val faq = AccountMobileQuickAction(
        key = "faq",
        title = stringResource(R.string.account_faq_group),
        icon = Icons.AutoMirrored.Outlined.HelpOutline,
        onClick = { onEvent(AccountState.Event.FaqSelected) },
    )
    val settings = AccountMobileQuickAction(
        key = "settings",
        title = stringResource(R.string.account_settings),
        icon = Icons.Outlined.Settings,
        onClick = { onEvent(AccountState.Event.SettingsSelected) },
    )
    val downloaded = AccountMobileQuickAction(
        key = "downloaded_episodes",
        title = stringResource(R.string.account_downloaded_episodes),
        icon = Icons.Outlined.VideoLibrary,
        onClick = { onEvent(AccountState.Event.DownloadedEpisodesSelected) },
        focusRequester = lastActionFocusRequester.takeUnless { state.isSignedIn },
    )
    if (!state.isSignedIn) return listOf(faq, settings, downloaded)

    val notifications = AccountMobileQuickAction(
        key = "notifications",
        title = stringResource(R.string.account_tab_notifications),
        icon = Icons.Outlined.Notifications,
        onClick = { onEvent(AccountState.Event.NotificationsSelected) },
        focusRequester = lastActionFocusRequester,
        badgeCount = state.unreadNotificationCount,
    )
    return listOf(faq, settings, downloaded, notifications)
}
