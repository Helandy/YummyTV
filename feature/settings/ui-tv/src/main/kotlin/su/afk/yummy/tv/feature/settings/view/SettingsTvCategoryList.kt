package su.afk.yummy.tv.feature.settings.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.model.SettingsTab

/** Левый вертикальный список категорий настроек (master); контент выбранной категории — справа. */
@Composable
internal fun SettingsTvCategoryList(
    selectedTab: SettingsTab,
    tabFocusRequesters: Map<SettingsTab, FocusRequester>,
    contentFocusRequesters: Map<SettingsTab, FocusRequester>,
    mainMenuFocusRequester: FocusRequester?,
    onSelectedTabChanged: (SettingsTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tabs = SettingsTab.entries

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { index, tab ->
            val tabContentFocusRequester = contentFocusRequesters.getValue(tab)
            SettingsCategoryItem(
                label = stringResource(tab.labelRes),
                selected = tab == selectedTab,
                contentFocusRequester = tabContentFocusRequester,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(tabFocusRequesters.getValue(tab)),
                upFocusRequester = tabFocusRequesters[tabs.getOrNull(index - 1)],
                downFocusRequester = tabFocusRequesters[tabs.getOrNull(index + 1)],
                leftFocusRequester = mainMenuFocusRequester,
                onSelected = { onSelectedTabChanged(tab) },
                onActivated = {
                    onSelectedTabChanged(tab)
                    runCatching { tabContentFocusRequester.requestFocus() }
                },
            )
        }
    }
}
