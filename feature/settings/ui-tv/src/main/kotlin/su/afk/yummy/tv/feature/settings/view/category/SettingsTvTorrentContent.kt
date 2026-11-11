package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker
import su.afk.yummy.tv.feature.settings.view.TorrentSettingsPanel

/** Категория «Торренты» на ТВ: настройки TorrServer и индексатора; сортировка выбирается в третьей колонке. */
@Composable
internal fun SettingsTvTorrentContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
    pickerRow: SettingsTvPickerRow,
) {
    TorrentSettingsPanel(
        settings = state.torrent,
        checkAvailable = state.torrServerCheckAvailable,
        checkStatus = state.torrServerCheck,
        onCheck = { onEvent(SettingsState.Event.TorrServerCheckRequested) },
        upFocusRequester = tabFocusRequester,
        contentFocusRequester = tabContentFocusRequester,
        onChange = { onEvent(SettingsState.Event.TorrentSettingsChanged(it)) },
        sortPickerRow = { modifier: Modifier -> pickerRow(SettingsTvPicker.TORRENT_SORT, modifier) },
    )
}
