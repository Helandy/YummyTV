package su.afk.yummy.tv.feature.account.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.LocalMobileMainActions
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.feature.account.account.mobile.utils.accountErrorMessage
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.view.AccountMobileEmptyText
import su.afk.yummy.tv.feature.account.view.AccountMobileHeader
import su.afk.yummy.tv.feature.account.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.view.AccountMobileLoginCard
import su.afk.yummy.tv.feature.account.view.AccountMobileLogoutConfirmDialog
import su.afk.yummy.tv.feature.account.view.AccountMobileNavigationButton
import su.afk.yummy.tv.feature.account.view.AccountMobileNotificationsTab
import su.afk.yummy.tv.feature.account.view.AccountMobileSettingsButton
import su.afk.yummy.tv.feature.account.view.AccountMobileStatsTab
import su.afk.yummy.tv.feature.account.view.AccountMobileTabs

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val mainActions = LocalMobileMainActions.current

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
                    item(key = "settings") {
                        AccountMobileSettingsButton(onClick = mainActions.onSettingsClick)
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
                    item(key = "settings") {
                        AccountMobileSettingsButton(onClick = mainActions.onSettingsClick)
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
                        unreadCount = state.notificationCounts.sumOf { it.count },
                        onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                    )
                }
                if (state.hubError != null) {
                    item(key = "hub_error") {
                        state.hubError.accountErrorMessage()?.let { error ->
                            AccountMobileEmptyText(text = error)
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
