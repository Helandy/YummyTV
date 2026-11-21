package su.afk.yummy.tv.feature.account.mobile.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.mobile.MobileCollapsingHeaderLayout
import su.afk.yummy.tv.core.designsystem.mobile.MobileSwipeableTabsPager
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileCollapsingHeaderState
import su.afk.yummy.tv.core.designsystem.mobile.rememberMobileSwipeableTabsState
import su.afk.yummy.tv.core.designsystem.mobile.state.MobileSectionLoading
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileHeader
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileStickyTabs
import su.afk.yummy.tv.feature.account.mobile.userprofile.view.UserProfileTabContent
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun UserProfileMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        UserProfileMobileScreen(UserProfileState.State(isOverviewLoading = false), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Loading", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
private fun UserProfileMobileScreenLoadingPreview() = ScreenPreviewTheme {
    UserProfileMobileScreen(UserProfileState.State(isOverviewLoading = true), emptyFlow()) {}
}

@Preview(name = "Error", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun UserProfileMobileScreenErrorPreview() = ScreenPreviewTheme {
    UserProfileMobileScreen(
        UserProfileState.State(isOverviewLoading = false, overviewError = true),
        emptyFlow(),
    ) {}
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun UserProfileMobileScreen(
    state: UserProfileState.State,
    effect: Flow<UserProfileState.Effect>,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val tabs = state.visibleTabs
    // Набор вкладок меняется после загрузки профиля (пустые скрываются), а pager держит
    // лямбды ключей/страниц дольше одной рекомпозиции — читаем всегда актуальный список.
    val currentTabs by rememberUpdatedState(tabs)
    val tabsState = rememberMobileSwipeableTabsState(
        selectedPage = tabs.indexOf(state.selectedTab).coerceAtLeast(0),
        pageCount = tabs.size,
        onPageSelected = { page ->
            currentTabs.getOrNull(page)?.let { onEvent(UserProfileState.Event.TabSelected(it)) }
        },
    )

    // Без топ-бара: баннер профиля заходит под статус-бар, «назад» — стрелкой на баннере, как в деталке.
    BaseScreen(
        contentModifier = Modifier.navigationBarsPadding(),
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal),
        isScroll = false,
    ) {
        // Шапка профиля уезжает вместе со списком вкладки, вкладки прилипают к верху —
        // иначе на списки остаётся пол-экрана.
        val headerState = rememberMobileCollapsingHeaderState()
        val statusBarTop = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val onBack = { onEvent(UserProfileState.Event.BackSelected) }
        Box(modifier = Modifier.weight(1f)) {
            MobileCollapsingHeaderLayout(
                modifier = Modifier.fillMaxSize(),
                state = headerState,
                collapsedTopInset = statusBarTop,
                header = {
                    UserProfileHeader(
                        state = state,
                        onEvent = onEvent,
                        onBack = onBack,
                    )
                },
                stickyHeader = {
                    UserProfileStickyTabs(
                        tabs = tabs,
                        selected = state.selectedTab,
                        profile = state.profile,
                        showBack = headerState.collapseFraction > 0.95f,
                        onBack = onBack,
                        onSelected = { tab -> tabsState.selectPage(tabs.indexOf(tab)) },
                        transition = tabsState::tabTransition,
                    )
                },
            ) {
                MobileSwipeableTabsPager(
                    state = tabsState,
                    modifier = Modifier.fillMaxSize(),
                    key = { page -> currentTabs.getOrNull(page)?.name ?: "page_$page" },
                ) { page ->
                    val tab = currentTabs.getOrNull(page)
                    if (tab != null && tab == state.selectedTab) {
                        UserProfileTabContent(
                            tab = tab,
                            state = state,
                            onEvent = onEvent,
                        )
                    } else {
                        MobileSectionLoading(modifier = Modifier.fillMaxSize())
                    }
                }
            }
            // Подложка статус-бара проявляется по мере схлопывания шапки.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(statusBarTop)
                    .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = headerState.collapseFraction)),
            )
        }
    }
}
