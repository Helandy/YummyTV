package su.afk.yummy.tv.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.view.AccountMobileEmptyText
import su.afk.yummy.tv.feature.account.view.AccountMobileHeader
import su.afk.yummy.tv.feature.account.view.AccountMobileLoginCard
import su.afk.yummy.tv.feature.account.view.AccountMobileLogoutConfirmDialog
import su.afk.yummy.tv.feature.account.view.AccountMobileNotificationsTab
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

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.account_mobile_title),
                onBack = { onEvent(AccountState.Event.BackSelected) },
            )
        },
    ) {
        if (!state.isSignedIn) {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 20.dp,
                    end = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    AccountMobileLoginCard(state = state, onEvent = onEvent)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 16.dp,
                    end = 16.dp,
                    bottom = 32.dp
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    AccountMobileHeader(
                        state = state,
                        onLogoutClick = { showLogoutConfirm = true },
                    )
                }
                item {
                    AccountMobileTabs(
                        selected = state.selectedTab,
                        unreadCount = state.notificationCounts.sumOf { it.count },
                        onSelected = { onEvent(AccountState.Event.TabSelected(it)) },
                    )
                }
                item {
                    state.hubError?.let { AccountMobileEmptyText(it) }
                }
                when (state.selectedTab) {
                    AccountState.AccountTab.STATS -> item {
                        AccountMobileStatsTab(
                            stats = state.stats,
                            isLoading = state.isStatsLoading,
                        )
                    }

                    AccountState.AccountTab.NOTIFICATIONS -> item {
                        AccountMobileNotificationsTab(
                            state = state,
                            onEvent = onEvent,
                        )
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
