package su.afk.yummy.tv.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.account.mobile.R
import su.afk.yummy.tv.feature.account.view.AccountMobileEmptyText
import su.afk.yummy.tv.feature.account.view.AccountMobileHeader
import su.afk.yummy.tv.feature.account.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.view.AccountMobileLoginCard
import su.afk.yummy.tv.feature.account.view.AccountMobileLogoutConfirmDialog
import su.afk.yummy.tv.feature.account.view.AccountMobileNotificationsTab
import su.afk.yummy.tv.feature.account.view.AccountMobileStatsTab
import su.afk.yummy.tv.feature.account.view.AccountMobileTabs

private const val ACCOUNT_MOBILE_PAGE_COUNT = 2
private const val ACCOUNT_MOBILE_STATS_PAGE = 0
private const val ACCOUNT_MOBILE_NOTIFICATIONS_PAGE = 1

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AccountMobileScreen(
    state: AccountState.State,
    effect: Flow<AccountState.Effect>,
    onEvent: (AccountState.Event) -> Unit,
) {
    var showLogoutConfirm by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(
        initialPage = state.selectedTab.toAccountMobilePage(),
        pageCount = { ACCOUNT_MOBILE_PAGE_COUNT },
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(state.selectedTab) {
        val targetPage = state.selectedTab.toAccountMobilePage()
        if (pagerState.currentPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val selectedTab = pagerState.currentPage.toAccountMobileTab()
        if (selectedTab != state.selectedTab) {
            onEvent(AccountState.Event.TabSelected(selectedTab))
        }
    }

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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
            ) {
                AccountMobileHeader(
                    state = state,
                    onLogoutClick = { showLogoutConfirm = true },
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                )
                AccountMobileTabs(
                    selected = pagerState.currentPage.toAccountMobileTab(),
                    unreadCount = state.notificationCounts.sumOf { it.count },
                    onSelected = { tab ->
                        val targetPage = tab.toAccountMobilePage()
                        if (pagerState.currentPage != targetPage) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(targetPage)
                            }
                        }
                    },
                    modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp),
                )
                state.hubError?.let { error ->
                    AccountMobileEmptyText(
                        text = error,
                        modifier = Modifier.padding(start = 16.dp, top = 14.dp, end = 16.dp),
                    )
                }
                HorizontalPager(
                    state = pagerState,
                    key = { page -> page.toAccountMobileTab() },
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) { page ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 14.dp,
                            end = 16.dp,
                            bottom = 32.dp,
                        ),
                    ) {
                        when (page.toAccountMobileTab()) {
                            AccountState.AccountTab.STATS -> {
                                if (state.isStatsLoading && state.stats == null) {
                                    item {
                                        AccountMobileLoadingIndicator(modifier = Modifier.fillParentMaxSize())
                                    }
                                } else {
                                    item {
                                        AccountMobileStatsTab(
                                            stats = state.stats,
                                            isLoading = state.isStatsLoading,
                                        )
                                    }
                                }
                            }

                            AccountState.AccountTab.NOTIFICATIONS -> {
                                if (state.isNotificationsLoading && state.notifications.isEmpty()) {
                                    item {
                                        AccountMobileLoadingIndicator(modifier = Modifier.fillParentMaxSize())
                                    }
                                } else {
                                    item {
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

private fun AccountState.AccountTab.toAccountMobilePage(): Int =
    when (this) {
        AccountState.AccountTab.STATS -> ACCOUNT_MOBILE_STATS_PAGE
        AccountState.AccountTab.NOTIFICATIONS -> ACCOUNT_MOBILE_NOTIFICATIONS_PAGE
    }

private fun Int.toAccountMobileTab(): AccountState.AccountTab =
    when (this) {
        ACCOUNT_MOBILE_NOTIFICATIONS_PAGE -> AccountState.AccountTab.NOTIFICATIONS
        else -> AccountState.AccountTab.STATS
    }
