package su.afk.yummy.tv.feature.settings.view.category

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.utils.toPreviewCacheSizeText
import su.afk.yummy.tv.feature.settings.view.AboutRow
import su.afk.yummy.tv.feature.settings.view.CacheStorageTvDialog
import su.afk.yummy.tv.feature.settings.view.SettingsDivider
import su.afk.yummy.tv.feature.settings.view.SettingsSectionTitle
import su.afk.yummy.tv.feature.settings.view.SettingsSliderRow

@Composable
internal fun SettingsTvStorageContent(
    state: SettingsState.State,
    tabFocusRequester: FocusRequester,
    tabContentFocusRequester: FocusRequester,
    onEvent: (SettingsState.Event) -> Unit,
) {
    val context = LocalContext.current
    var showCacheStorageDialog by remember { mutableStateOf(false) }

    SettingsSectionTitle(text = stringResource(R.string.settings_poster_cache_size_title))
    SettingsSliderRow(
        label = stringResource(R.string.settings_poster_cache_size_title),
        valueText = state.previewCacheSize.toPreviewCacheSizeText(),
        value = state.previewCacheSize,
        valueRange = 50..500,
        stepSize = 50,
        enabled = true,
        onValueChange = {
            onEvent(SettingsState.Event.PreviewCacheSizeSelected(it))
        },
        modifier = Modifier
            .focusRequester(tabContentFocusRequester)
            .restoreCategoryFocusOnLeft(tabFocusRequester),
    )
    SettingsDivider()
    AboutRow(
        label = stringResource(R.string.settings_cache_storage_title),
        hint = Formatter.formatShortFileSize(context, state.cacheStorageTotalBytes),
        onClick = {
            onEvent(SettingsState.Event.CacheStorageRefreshRequested)
            showCacheStorageDialog = true
        },
    )
    if (showCacheStorageDialog) {
        CacheStorageTvDialog(
            entries = state.cacheStorageEntries,
            totalBytes = state.cacheStorageTotalBytes,
            isLoading = state.isCacheStorageLoading,
            onRefresh = { onEvent(SettingsState.Event.CacheStorageRefreshRequested) },
            onDismiss = { showCacheStorageDialog = false },
        )
    }
}
