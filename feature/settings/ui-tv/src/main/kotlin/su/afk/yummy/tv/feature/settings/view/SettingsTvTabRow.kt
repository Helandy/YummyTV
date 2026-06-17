package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.model.SettingsTab

@Composable
internal fun SettingsTvTabRow(
    selectedTab: SettingsTab,
    tabFocusRequesters: Map<SettingsTab, FocusRequester>,
    contentFocusRequesters: Map<SettingsTab, FocusRequester>,
    mainMenuFocusRequester: FocusRequester?,
    onSelectedTabChanged: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = SettingsTab.entries
    val scrollState = rememberScrollState()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .focusGroup()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEachIndexed { index, tab ->
            val tabContentFocusRequester = contentFocusRequesters.getValue(tab)
            SettingsTabItem(
                label = stringResource(tab.labelRes),
                selected = tab == selectedTab,
                modifier = Modifier.focusRequester(tabFocusRequesters.getValue(tab)),
                contentFocusRequester = tabContentFocusRequester,
                leftFocusRequester = tabFocusRequesters[tabs.getOrNull(index - 1)]
                    ?: mainMenuFocusRequester.takeIf { index == 0 },
                rightFocusRequester = tabFocusRequesters[tabs.getOrNull(index + 1)]
                    ?: tabContentFocusRequester,
                onSelected = {
                    onSelectedTabChanged(tab)
                },
                onActivated = {
                    onSelectedTabChanged(tab)
                    runCatching { tabContentFocusRequester.requestFocus() }
                },
            )
        }
    }
}
