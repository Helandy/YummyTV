package su.afk.yummy.tv.feature.library.mobile.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileChipTab
import su.afk.yummy.tv.core.designsystem.mobile.tabs.MobileChipTabs
import su.afk.yummy.tv.feature.library.mobile.utils.mobileTitle
import su.afk.yummy.tv.feature.library.mobile.utils.tabColor
import su.afk.yummy.tv.feature.library.mobile.utils.tabIcon
import su.afk.yummy.tv.feature.library.model.LibraryTab

@Composable
internal fun LibraryMobileTabs(
    tabs: List<LibraryTab>,
    selectedTab: LibraryTab,
    tabCounts: Map<LibraryTab, Int?>,
    onSelected: (LibraryTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    MobileChipTabs(
        tabs = tabs.map { tab ->
            MobileChipTab(
                key = tab,
                title = tab.mobileTitle(),
                icon = tab.tabIcon(),
                count = tabCounts[tab],
                color = tab.tabColor(),
            )
        },
        selectedKey = selectedTab,
        onSelected = onSelected,
        modifier = modifier,
    )
}
