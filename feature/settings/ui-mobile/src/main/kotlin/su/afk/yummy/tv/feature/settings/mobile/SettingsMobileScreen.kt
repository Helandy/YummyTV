package su.afk.yummy.tv.feature.settings.mobile

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.core.utils.restartApplication
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePickerOption
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.mobile.view.MobileInterfaceModeConfirmationDialog
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileAboutRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileActionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileNavigationRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobilePickerSheet
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileToggleRow

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SettingsMobileScreenDefaultPreview() =
    ScreenPreviewTheme {
        SettingsMobileScreen(SettingsState.State(), emptyFlow()) {}
    }

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsMobileScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    var activePicker by remember { mutableStateOf<SettingsMobilePicker?>(null) }
    var pendingInterfaceMode by remember { mutableStateOf<AppInterfaceMode?>(null) }
    val title = stringResource(R.string.settings_mobile_title)
    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.settings_repository_url)
    val interfaceModeFocusRequester = remember { FocusRequester() }
    val videoExportDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            onEvent(SettingsState.Event.VideoExportDirectoryGranted(it.toString()))
        }
    }

    LaunchedEffect(Unit) {
        repeat(3) {
            withFrameNanos { }
            if (runCatching { interfaceModeFocusRequester.requestFocus() }.getOrDefault(false)) {
                return@LaunchedEffect
            }
        }
    }

    LaunchedEffect(effect) {
        effect.collect { settingsEffect ->
            when (settingsEffect) {
                SettingsState.Effect.RestartApplication -> {
                    if (!context.restartApplication()) {
                        Toast.makeText(
                            context,
                            R.string.settings_interface_restart_failed,
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }

                SettingsState.Effect.OpenVideoExportDirectoryPicker ->
                    videoExportDirectoryPicker.launch(null)

                SettingsState.Effect.VideoExportDirectorySelectionFailed ->
                    Toast.makeText(
                        context,
                        R.string.settings_video_export_directory_error,
                        Toast.LENGTH_LONG,
                    ).show()
            }
        }
    }

    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = title,
                onBack = { onEvent(SettingsState.Event.BackSelected) },
            )
        },
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                SettingsMobileSection(
                    title = stringResource(R.string.settings_mobile_section_interface),
                ) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_interface_type),
                        value = state.interfaceMode.label(),
                        hint = state.interfaceMode.hint(),
                        onClick = { activePicker = SettingsMobilePicker.INTERFACE_MODE },
                        modifier = Modifier.focusRequester(interfaceModeFocusRequester),
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_appearance)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_theme),
                        value = state.appTheme.label(),
                        hint = state.appTheme.hint(),
                        onClick = { activePicker = SettingsMobilePicker.THEME },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_poster_size),
                        value = state.posterCardSize.label(),
                        hint = state.posterCardSize.hint(),
                        onClick = { activePicker = SettingsMobilePicker.POSTER_SIZE },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_library_continue_watching_card_size_title),
                        value = state.libraryContinueWatchingCardSize.label(),
                        hint = state.libraryContinueWatchingCardSize.hint(),
                        onClick = {
                            activePicker = SettingsMobilePicker.LIBRARY_CONTINUE_WATCHING_SIZE
                        },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_poster_quality),
                        value = state.posterQuality.label(),
                        hint = state.posterQuality.hint(),
                        onClick = { activePicker = SettingsMobilePicker.POSTER_QUALITY },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_show_top_title_year),
                        hint = if (state.showTopTitleYear) {
                            stringResource(R.string.settings_show_top_title_year_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.showTopTitleYear,
                        onClick = { onEvent(SettingsState.Event.ShowTopTitleYearToggled) },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_player)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_default_player),
                        value = state.preferredPlayer.label(),
                        hint = state.preferredPlayer.hint(),
                        onClick = { activePicker = SettingsMobilePicker.PLAYER },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_preferred_video_quality_title),
                        value = state.preferredVideoQuality.label(),
                        hint = state.preferredVideoQuality.hint(),
                        onClick = { activePicker = SettingsMobilePicker.VIDEO_QUALITY },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_auto_skip_label),
                        hint = if (state.autoSkipOpeningsEndings) {
                            stringResource(R.string.settings_auto_skip_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.autoSkipOpeningsEndings,
                        onClick = { onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled) },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_auto_play_next_episode_label),
                        hint = if (state.autoPlayNextEpisode) {
                            stringResource(R.string.settings_auto_play_next_episode_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.autoPlayNextEpisode,
                        onClick = { onEvent(SettingsState.Event.AutoPlayNextEpisodeToggled) },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_picture_in_picture_label),
                        hint = if (state.pictureInPictureEnabled) {
                            stringResource(R.string.settings_picture_in_picture_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.pictureInPictureEnabled,
                        onClick = { onEvent(SettingsState.Event.PictureInPictureToggled) },
                    )
                    SettingsMobileActionRow(
                        label = stringResource(R.string.settings_player_gesture_tutorial_reset),
                        hint = if (state.mobilePlayerGestureTutorialDismissed) {
                            stringResource(R.string.settings_player_gesture_tutorial_reset_hint)
                        } else {
                            stringResource(R.string.settings_player_gesture_tutorial_reset_done)
                        },
                        enabled = state.mobilePlayerGestureTutorialDismissed,
                        onClick = {
                            onEvent(SettingsState.Event.MobilePlayerGestureTutorialReset)
                        },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_suggest_next_episode_on_watched_label),
                        hint = if (state.suggestNextEpisodeOnWatched) {
                            stringResource(R.string.settings_suggest_next_episode_on_watched_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.suggestNextEpisodeOnWatched,
                        onClick = {
                            onEvent(SettingsState.Event.SuggestNextEpisodeOnWatchedToggled)
                        },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_refresh_continue_watching_progress_label),
                        hint = if (state.refreshContinueWatchingProgressOnLaunch) {
                            stringResource(R.string.settings_refresh_continue_watching_progress_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.refreshContinueWatchingProgressOnLaunch,
                        onClick = {
                            onEvent(
                                SettingsState.Event.RefreshContinueWatchingProgressOnLaunchToggled,
                            )
                        },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_details)) {
                    SettingsMobileNavigationRow(
                        label = stringResource(R.string.settings_details_buttons_order),
                        hint = stringResource(R.string.settings_details_buttons_order_hint),
                        onClick = { onEvent(SettingsState.Event.DetailsButtonOrderSelected) },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_cache)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_mobile_preview_cache),
                        value = state.previewCacheSize.label(),
                        hint = state.previewCacheSize.hint(),
                        onClick = { activePicker = SettingsMobilePicker.CACHE },
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
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_language)) {
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_content_language_title),
                        value = state.contentLanguage.label(),
                        onClick = { activePicker = SettingsMobilePicker.CONTENT_LANGUAGE },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_api)) {
                    OutlinedTextField(
                        value = state.yaniApplicationToken,
                        onValueChange = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        label = { Text(stringResource(R.string.settings_yani_application_token_label)) },
                        placeholder = { Text(stringResource(R.string.settings_yani_application_token_placeholder)) },
                        supportingText = { Text(stringResource(R.string.settings_yani_application_token_hint)) },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_about)) {
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_version_label),
                        hint = BuildConfig.VERSION_NAME,
                    )
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_feedback_label),
                        hint = repositoryUrl,
                        onClick = { context.openExternalUri(repositoryUrl) },
                    )
                }
            }
        }
    }

    when (activePicker) {
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
            onDismiss = { activePicker = null },
            onSelected = { selectedMode ->
                activePicker = null
                if (selectedMode != state.interfaceMode) {
                    pendingInterfaceMode = selectedMode
                }
            },
        )

        SettingsMobilePicker.THEME -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_theme),
            selectedValue = state.appTheme,
            options = AppTheme.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.AppThemeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.POSTER_QUALITY -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_poster_quality),
            selectedValue = state.posterQuality,
            options = PosterQuality.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PosterQualitySelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.POSTER_SIZE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_poster_size),
            selectedValue = state.posterCardSize,
            options = PosterCardSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PosterCardSizeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.LIBRARY_CONTINUE_WATCHING_SIZE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_library_continue_watching_card_size_title),
            selectedValue = state.libraryContinueWatchingCardSize,
            options = LibraryContinueWatchingCardSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.LibraryContinueWatchingCardSizeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.PLAYER -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_default_player),
            selectedValue = state.preferredPlayer,
            options = PreferredPlayer.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PreferredPlayerSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.VIDEO_QUALITY -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_preferred_video_quality_title),
            selectedValue = state.preferredVideoQuality,
            options = PreferredVideoQuality.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PreferredVideoQualitySelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.CACHE -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_mobile_preview_cache),
            selectedValue = state.previewCacheSize,
            options = PreviewCacheSize.entries.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint()
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PreviewCacheSizeSelected(it))
                activePicker = null
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
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.ContentLanguageSelected(it))
                activePicker = null
            },
        )

        null -> Unit
    }

    pendingInterfaceMode?.let { targetMode ->
        MobileInterfaceModeConfirmationDialog(
            targetModeLabel = targetMode.label(),
            onConfirm = {
                pendingInterfaceMode = null
                onEvent(SettingsState.Event.InterfaceModeSelected(targetMode))
            },
            onDismiss = { pendingInterfaceMode = null },
        )
    }
}
