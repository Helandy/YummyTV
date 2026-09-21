package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.view.ApiSettingsPanel

@Composable
internal fun SettingsTvApiContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
) {
    ApiSettingsPanel(
        token = state.yaniApplicationToken,
        upFocusRequester = tabFocusRequester,
        contentFocusRequester = tabContentFocusRequester,
        onTokenChanged = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
    )
}
