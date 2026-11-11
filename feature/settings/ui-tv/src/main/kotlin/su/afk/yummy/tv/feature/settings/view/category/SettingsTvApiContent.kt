package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.view.ApiSettingsPanel
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.TorrentSettingsPanel

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
        canReset = state.isYaniApplicationTokenCustom,
        onTokenChanged = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
        onReset = { onEvent(SettingsState.Event.YaniApplicationTokenChanged("")) },
    )
    SettingsDivider()
    TorrentSettingsPanel(
        settings = state.torrent,
        upFocusRequester = tabFocusRequester,
        onChange = { onEvent(SettingsState.Event.TorrentSettingsChanged(it)) },
    )
}
