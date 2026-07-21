package su.afk.yummy.tv.feature.account.mobile.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.LocalMobileBottomBarUpFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.mobile.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.account.AccountState
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.mobile.account.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileFaqButton
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileHeader
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoginCard
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLogoutConfirmDialog
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileNavigationButton
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileNotificationsTab
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileSettingsButton
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileSitePagesButton
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
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val mainActions = LocalMobileMainActions.current
    val bottomBarUpFocusRequester = LocalMobileBottomBarUpFocusRequester.current

    BaseScreen(
        isScroll = false,
    ) {
        if (!state.isSignedIn) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = MobileBottomBarDefaults.ContentBottomPadding +
                            MobileBottomBarDefaults.ExtraContentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (mainActions != null) {
                    item(key = "faq") {
                        AccountMobileFaqButton(onClick = mainActions.onFaqClick)
                    }
                    item(key = "site_pages") {
                        AccountMobileSitePagesButton(onClick = mainActions.onSitePagesClick)
                    }
                    item(key = "settings") {
                        AccountMobileSettingsButton(
                            onClick = mainActions.onSettingsClick,
                            focusRequester = bottomBarUpFocusRequester,
                        )
                    }
                }
                item(key = "downloaded_episodes") {
                    AccountMobileNavigationButton(
                        title = stringResource(R.string.account_downloaded_episodes),
                        icon = Icons.Filled.VideoLibrary,
                        onClick = {
                            onEvent(AccountState.Event.DownloadedEpisodesSelected)
                        },
                    )
                }
                item(key = "user_search") {
                    AccountMobileNavigationButton(
                        title = stringResource(R.string.account_user_search),
                        icon = Icons.Filled.PersonSearch,
                        onClick = { onEvent(AccountState.Event.UserSearchSelected) },
                    )
                }
                item {
                    AccountMobileLoginCard(state = state, onEvent = onEvent)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 12.dp,
                    end = 16.dp,
                    bottom = MobileBottomBarDefaults.ContentBottomPadding +
                            MobileBottomBarDefaults.ExtraContentBottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (mainActions != null) {
                    item(key = "faq") {
                        AccountMobileFaqButton(onClick = mainActions.onFaqClick)
                    }
                    item(key = "site_pages") {
                        AccountMobileSitePagesButton(onClick = mainActions.onSitePagesClick)
                    }
                    item(key = "settings") {
                        AccountMobileSettingsButton(
                            onClick = mainActions.onSettingsClick,
                            focusRequester = bottomBarUpFocusRequester,
                        )
                    }
                }
                item(key = "downloaded_episodes") {
                    AccountMobileNavigationButton(
                        title = stringResource(R.string.account_downloaded_episodes),
                        icon = Icons.Filled.VideoLibrary,
                        onClick = {
                            onEvent(AccountState.Event.DownloadedEpisodesSelected)
                        },
                    )
                }
                item(key = "user_search") {
                    AccountMobileNavigationButton(
                        title = stringResource(R.string.account_user_search),
                        icon = Icons.Filled.PersonSearch,
                        onClick = { onEvent(AccountState.Event.UserSearchSelected) },
                    )
                }
                item(key = "messages") {
                    AccountMobileNavigationButton(
                        title = stringResource(R.string.account_messages),
                        icon = Icons.Filled.Email,
                        onClick = { onEvent(AccountState.Event.MessagesSelected) },
                    )
                }
                item(key = "profile") {
                    AccountMobileHeader(
                        state = state,
                        profileSummary = state.profileSummary,
                        onLogoutClick = { showLogoutConfirm = true },
                    )
                }
                item(key = "tabs") {
                    AccountMobileTabs(
                        selected = state.selectedTab,
                        unreadCount = state.unreadNotificationCount,
                        onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
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
                item(key = "selected_tab_${state.selectedTab}") {
                    when (state.selectedTab) {
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
