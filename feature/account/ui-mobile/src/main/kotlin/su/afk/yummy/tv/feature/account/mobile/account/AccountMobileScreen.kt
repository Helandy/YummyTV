package su.afk.yummy.tv.feature.account.mobile.account

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WatchLater
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.mobile.bar.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileMessage
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileHeader
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoginCard
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLogoutConfirmDialog
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileNotificationsTab
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileQuickAction
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileQuickActionsGrid
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileStatsTab
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileTabs
import su.afk.yummy.tv.core.designsystem.R as CoreR

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun AccountMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        AccountMobileScreen(AccountState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun AccountMobileScreenLoadingPreview() = ScreenPreviewTheme {
    AccountMobileScreen(AccountState.State(isLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun AccountMobileScreenErrorPreview() = ScreenPreviewTheme {
    AccountMobileScreen(
        AccountState.State(error = su.afk.yummy.tv.feature.account.account.model.AccountUiError.SIGN_IN_FAILED),
        emptyFlow(),
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val captchaHint = stringResource(R.string.account_captcha_required_toast)
    LaunchedEffect(Unit) { onEvent(AccountState.Event.ScreenShown) }

    LaunchedEffect(effect) {
        effect.collect { effectItem ->
            when (effectItem) {
                AccountState.Effect.HideKeyboard -> {
                    keyboardController?.hide()
                    focusManager.clearFocus()
                }

                AccountState.Effect.ShowCaptchaHint -> Toast.makeText(context, captchaHint, Toast.LENGTH_SHORT).show()
            }
        }
    }

    var showLogoutConfirm by remember { mutableStateOf(false) }
    val mainActions = LocalMobileMainActions.current
    val bottomBarUpFocusRequester = LocalMobileBottomBarUpFocusRequester.current
    val accountTabs = AccountState.AccountTab.entries
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = accountTabs.indexOf(state.selectedTab).coerceAtLeast(0),
        pageCount = accountTabs.size,
        onPageSelected = { page ->
            accountTabs.getOrNull(page)?.let { onEvent(AccountState.Event.TabSelected(it)) }
        },
    )

    BaseScreen(
        isScroll = false,
    ) {
        if (!state.isSignedIn) {
            val quickActions = buildList {
                if (mainActions != null) {
                    add(
                        AccountMobileQuickAction(
                            key = "faq",
                            title = stringResource(R.string.account_faq),
                            icon = Icons.Filled.Info,
                            onClick = mainActions.onFaqClick,
                        ),
                    )
                    add(
                        AccountMobileQuickAction(
                            key = "site_pages",
                            title = stringResource(R.string.account_site_pages),
                            icon = Icons.Filled.Language,
                            onClick = mainActions.onSitePagesClick,
                        ),
                    )
                    add(
                        AccountMobileQuickAction(
                            key = "settings",
                            title = stringResource(R.string.account_settings),
                            icon = Icons.Filled.Settings,
                            onClick = mainActions.onSettingsClick,
                            focusRequester = bottomBarUpFocusRequester,
                        ),
                    )
                }
                add(
                    AccountMobileQuickAction(
                        key = "user_search",
                        title = stringResource(R.string.account_user_search),
                        icon = Icons.Filled.PersonSearch,
                        onClick = { onEvent(AccountState.Event.UserSearchSelected) },
                    ),
                )
                add(
                    AccountMobileQuickAction(
                        key = "watch_later",
                        title = stringResource(R.string.account_watch_later),
                        icon = Icons.Filled.WatchLater,
                        onClick = { onEvent(AccountState.Event.WatchLaterSelected) },
                    ),
                )
                add(
                    AccountMobileQuickAction(
                        key = "downloaded_episodes",
                        title = stringResource(R.string.account_downloaded_episodes),
                        icon = Icons.Filled.VideoLibrary,
                        onClick = { onEvent(AccountState.Event.DownloadedEpisodesSelected) },
                    ),
                )
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = MobileBottomBarDefaults.contentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "quick_actions") {
                    AccountMobileQuickActionsGrid(actions = quickActions)
                }
                item {
                    AccountMobileLoginCard(state = state, onEvent = onEvent)
                }
            }
        } else {
            val quickActions = buildList {
                if (mainActions != null) {
                    add(
                        AccountMobileQuickAction(
                            key = "faq",
                            title = stringResource(R.string.account_faq),
                            icon = Icons.Filled.Info,
                            onClick = mainActions.onFaqClick,
                        ),
                    )
                    add(
                        AccountMobileQuickAction(
                            key = "site_pages",
                            title = stringResource(R.string.account_site_pages),
                            icon = Icons.Filled.Language,
                            onClick = mainActions.onSitePagesClick,
                        ),
                    )
                    add(
                        AccountMobileQuickAction(
                            key = "settings",
                            title = stringResource(R.string.account_settings),
                            icon = Icons.Filled.Settings,
                            onClick = mainActions.onSettingsClick,
                            focusRequester = bottomBarUpFocusRequester,
                        ),
                    )
                }
                add(
                    AccountMobileQuickAction(
                        key = "user_search",
                        title = stringResource(R.string.account_user_search),
                        icon = Icons.Filled.PersonSearch,
                        onClick = { onEvent(AccountState.Event.UserSearchSelected) },
                    ),
                )
                add(
                    AccountMobileQuickAction(
                        key = "messages",
                        title = stringResource(R.string.account_messages),
                        icon = Icons.Filled.Email,
                        onClick = { onEvent(AccountState.Event.MessagesSelected) },
                    ),
                )
                add(
                    AccountMobileQuickAction(
                        key = "watch_later",
                        title = stringResource(R.string.account_watch_later),
                        icon = Icons.Filled.WatchLater,
                        onClick = { onEvent(AccountState.Event.WatchLaterSelected) },
                    ),
                )
                add(
                    AccountMobileQuickAction(
                        key = "downloaded_episodes",
                        title = stringResource(R.string.account_downloaded_episodes),
                        icon = Icons.Filled.VideoLibrary,
                        onClick = { onEvent(AccountState.Event.DownloadedEpisodesSelected) },
                    ),
                )
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = MobileBottomBarDefaults.contentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "profile") {
                    AccountMobileHeader(
                        state = state,
                        profileSummary = state.profileSummary,
                        onEditClick = { onEvent(AccountState.Event.ProfileEditSelected) },
                        onLogoutClick = { showLogoutConfirm = true },
                    )
                }
                item(key = "quick_actions") {
                    AccountMobileQuickActionsGrid(actions = quickActions)
                }
                item(key = "tabs") {
                    AccountMobileTabs(
                        selected = state.selectedTab,
                        unreadCount = state.unreadNotificationCount,
                        onSelected = { tab -> tabsState.selectPage(accountTabs.indexOf(tab)) },
                    )
                }
                if (state.hubError != null) {
                    item(key = "hub_error") {
                        state.hubError.accountErrorMessage()?.let { error ->
                            MobileMessage(
                                title = error,
                                icon = Icons.Filled.Warning,
                                actionLabel = stringResource(CoreR.string.retry),
                                onAction = { onEvent(AccountState.Event.RefreshHubSelected) },
                                fillMaxSize = false,
                            )
                        }
                    }
                }
                item(key = "tabs_pager") {
                    MobileSwipeableTabsPager(
                        state = tabsState,
                        modifier = Modifier.fillMaxWidth(),
                        key = { page -> accountTabs[page].name },
                    ) { page ->
                        when (accountTabs[page]) {
                            AccountState.AccountTab.STATS -> {
                                if (state.isStatsLoading && state.stats == null && state.profileSummary == null) {
                                    AccountMobileLoadingIndicator()
                                } else {
                                    AccountMobileStatsTab(
                                        profileSummary = state.profileSummary,
                                        stats = state.stats,
                                        isLoading = state.isStatsLoading,
                                    )
                                }
                            }

                            AccountState.AccountTab.NOTIFICATIONS -> {
                                if (state.isNotificationsLoading && state.notifications.isEmpty()) {
                                    AccountMobileLoadingIndicator()
                                } else {
                                    AccountMobileNotificationsTab(
                                        state = state,
                                        onEvent = onEvent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showLogoutConfirm) {
        AccountMobileLogoutConfirmDialog(
            onConfirm = {
                showLogoutConfirm = false
                onEvent(AccountState.Event.LogoutSelected)
            },
            onDismiss = { showLogoutConfirm = false },
        )
    }
}
