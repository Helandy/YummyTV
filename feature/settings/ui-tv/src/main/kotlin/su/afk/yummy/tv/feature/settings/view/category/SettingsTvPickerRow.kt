package su.afk.yummy.tv.feature.settings.view.category

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import su.afk.yummy.tv.feature.settings.model.SettingsTvPicker

/** Строка пункта-пикера в центральной колонке; экран сам знает, открыт ли пункт и куда вернуть фокус. */
internal typealias SettingsTvPickerRow = @Composable (picker: SettingsTvPicker, modifier: Modifier) -> Unit
