package su.afk.yummy.tv.feature.settings.mobile.view.category

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.utils.toPreviewCacheSizeText
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSliderRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow

@Composable
internal fun SettingsMobileStorageContent(
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
    onCacheStorageRequested: () -> Unit,
) {
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SettingsMobileSection {
            SettingsMobileSliderRow(
                label = stringResource(R.string.settings_mobile_preview_cache),
                valueText = state.previewCacheSize.toPreviewCacheSizeText(),
                value = state.previewCacheSize,
                valueRange = 50..500,
                stepSize = 50,
                enabled = true,
                onValueChange = {
                    onEvent(SettingsState.Event.PreviewCacheSizeSelected(it))
                },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_video_export_directory),
                value = state.videoExportDirectoryName
                    ?: stringResource(R.string.settings_video_export_directory_not_selected),
                hint = stringResource(R.string.settings_video_export_directory_hint),
                onClick = {
                    onEvent(SettingsState.Event.VideoExportDirectorySelected)
                },
            )
            SettingsMobileToggleRow(
                label = stringResource(R.string.settings_video_export_auto_label),
                hint = if (state.videoExportAutoEnabled) {
                    stringResource(R.string.settings_video_export_auto_enabled)
                } else {
                    stringResource(R.string.settings_disabled)
                },
                enabled = state.videoExportAutoEnabled,
                onClick = { onEvent(SettingsState.Event.VideoExportAutoToggled) },
            )
            SettingsMobileOptionRow(
                label = stringResource(R.string.settings_cache_storage_title),
                value = Formatter.formatShortFileSize(
                    context,
                    state.cacheStorageTotalBytes,
                ),
                hint = stringResource(R.string.settings_cache_storage_button_hint),
                onClick = {
                    onEvent(SettingsState.Event.CacheStorageRefreshRequested)
                    onCacheStorageRequested()
                },
            )
        }
    }
}
