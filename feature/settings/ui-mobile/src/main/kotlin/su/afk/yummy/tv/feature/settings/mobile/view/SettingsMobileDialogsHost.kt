package su.afk.yummy.tv.feature.settings.mobile.view

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.R
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePickerOption
import su.afk.yummy.tv.feature.settings.mobile.utils.availableAppThemes
import su.afk.yummy.tv.feature.settings.mobile.utils.color
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label

/** Состояние шторок и диалогов, общее для всех категорий мобильных настроек. */
@Stable
internal class SettingsMobileDialogs {
    var activePicker by mutableStateOf<SettingsMobilePicker?>(null)
    var pendingInterfaceMode by mutableStateOf<AppInterfaceMode?>(null)
    var showCacheStorage by mutableStateOf(false)
}

@Composable
internal fun rememberSettingsMobileDialogs(): SettingsMobileDialogs = remember { SettingsMobileDialogs() }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun SettingsMobileDialogsHost(
    dialogs: SettingsMobileDialogs,
    state: SettingsState.State,
    onEvent: (SettingsState.Event) -> Unit,
) {
    when (dialogs.activePicker) {
        SettingsMobilePicker.INTERFACE_MODE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_interface_type),
            selectedValue = state.interfaceMode,
            options = AppInterfaceMode.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = { selectedMode ->
                dialogs.activePicker = null
                if (selectedMode != state.interfaceMode) {
                    dialogs.pendingInterfaceMode = selectedMode
                }
            },
        )

        SettingsMobilePicker.THEME -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_theme),
            selectedValue = state.appTheme,
            options = availableAppThemes.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.AppThemeSelected(it))
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.BACKGROUND -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_background),
            selectedValue = state.backgroundStyle,
            options = BackgroundStyle.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.BackgroundStyleSelected(it))
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.POSTER_QUALITY -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_poster_quality),
            selectedValue = state.posterQuality,
            options = PosterQuality.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PosterQualitySelected(it))
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.POSTER_SIZE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_poster_size),
            selectedValue = state.posterCardSize,
            options = PosterCardSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PosterCardSizeSelected(it))
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.LIBRARY_CONTINUE_WATCHING_SIZE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_library_continue_watching_card_size_title),
            selectedValue = state.libraryContinueWatchingCardSize,
            options = LibraryContinueWatchingCardSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.LibraryContinueWatchingCardSizeSelected(it))
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.PLAYER -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_default_player),
            selectedValue = state.preferredPlayer,
            options = PreferredPlayer.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PreferredPlayerSelected(it))
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.PLAYER_ORIENTATION -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_player_orientation_title),
            selectedValue = state.playerOrientationMode,
            options = PlayerOrientationMode.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PlayerOrientationModeSelected(it))
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.SUBTITLE_COLOR -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_subtitle_color_title),
            selectedValue = state.subtitleStyle.textColor,
            options = PlayerSubtitleTextColor.entries.map {
                SettingsMobilePickerOption(it, it.label(), labelColor = it.color)
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(
                    SettingsState.Event.SubtitleStyleSelected(
                        state.subtitleStyle.copy(textColor = it),
                    ),
                )
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.SUBTITLE_BACKGROUND -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_subtitle_background_title),
            selectedValue = state.subtitleStyle.background,
            options = PlayerSubtitleBackground.entries.map {
                SettingsMobilePickerOption(it, it.label())
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(
                    SettingsState.Event.SubtitleStyleSelected(
                        state.subtitleStyle.copy(background = it),
                    ),
                )
                dialogs.activePicker = null
            },
        )

        SettingsMobilePicker.CONTENT_LANGUAGE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_content_language_title),
            selectedValue = state.contentLanguage,
            options = YaniContentLanguage.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                )
            },
            onDismiss = { dialogs.activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.ContentLanguageSelected(it))
                dialogs.activePicker = null
            },
        )

        null -> Unit
    }

    if (dialogs.showCacheStorage) {
        CacheStorageMobileDialog(
            entries = state.cacheStorageEntries,
            totalBytes = state.cacheStorageTotalBytes,
            isLoading = state.isCacheStorageLoading,
            onRefresh = { onEvent(SettingsState.Event.CacheStorageRefreshRequested) },
            onDismiss = { dialogs.showCacheStorage = false },
        )
    }

    dialogs.pendingInterfaceMode?.let { targetMode ->
        MobileInterfaceModeConfirmationDialog(
            targetModeLabel = targetMode.label(),
            onConfirm = {
                dialogs.pendingInterfaceMode = null
                onEvent(SettingsState.Event.InterfaceModeSelected(targetMode))
            },
            onDismiss = { dialogs.pendingInterfaceMode = null },
        )
    }
}
