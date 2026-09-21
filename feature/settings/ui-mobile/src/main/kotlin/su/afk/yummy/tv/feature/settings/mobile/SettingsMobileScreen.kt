package su.afk.yummy.tv.feature.settings.mobile

import android.text.format.Formatter
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.mobile.bar.MobileTopBar
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.core.utils.system.restartApplication
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePicker
import su.afk.yummy.tv.feature.settings.mobile.model.SettingsMobilePickerOption
import su.afk.yummy.tv.feature.settings.mobile.utils.availableAppThemes
import su.afk.yummy.tv.feature.settings.mobile.utils.color
import su.afk.yummy.tv.feature.settings.mobile.utils.detailsText
import su.afk.yummy.tv.feature.settings.mobile.utils.hint
import su.afk.yummy.tv.feature.settings.mobile.utils.label
import su.afk.yummy.tv.feature.settings.mobile.utils.toNextEpisodeSwitchDelayText
import su.afk.yummy.tv.feature.settings.mobile.utils.toPreviewCacheSizeText
import su.afk.yummy.tv.feature.settings.mobile.utils.toSubtitlePercentText
import su.afk.yummy.tv.feature.settings.mobile.utils.videoQualitySliderEntries
import su.afk.yummy.tv.feature.settings.mobile.view.CacheStorageMobileDialog
import su.afk.yummy.tv.feature.settings.mobile.view.MobileInterfaceModeConfirmationDialog
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileAboutRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileActionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileNavigationRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileOptionRow
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobilePickerSheet
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSection
import su.afk.yummy.tv.feature.settings.mobile.view.SettingsMobileSliderRow
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
    var showCacheStorageDialog by remember { mutableStateOf(false) }
    val title = stringResource(R.string.settings_mobile_title)
    val context = LocalContext.current
    val repositoryUrl = stringResource(R.string.settings_repository_url)
    val interfaceModeFocusRequester = remember { FocusRequester() }
    val videoExportDirectoryPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        uri?.let {
            onEvent(SettingsState.Event.VideoExportDirectoryGranted(it.toString()))
        }
    }

    LaunchedEffect(Unit) {
        requestFocusUntilTimeout(interfaceModeFocusRequester)
    }

    LaunchedEffect(Unit) {
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
            modifier = Modifier.imePadding(),
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
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_search)) {
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_save_last_search_label),
                        hint = if (state.saveLastSearchEnabled) {
                            stringResource(R.string.settings_save_last_search_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.saveLastSearchEnabled,
                        onClick = { onEvent(SettingsState.Event.SaveLastSearchToggled) },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_devices)) {
                    SettingsMobileNavigationRow(
                        label = stringResource(R.string.settings_mobile_login_on_tv),
                        hint = stringResource(R.string.settings_mobile_login_on_tv_hint),
                        onClick = { onEvent(SettingsState.Event.LoginOnTvSelected) },
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
                        label = stringResource(R.string.settings_mobile_background),
                        value = state.backgroundStyle.label(),
                        hint = state.backgroundStyle.hint(),
                        onClick = { activePicker = SettingsMobilePicker.BACKGROUND },
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
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_show_library_title_year),
                        hint = if (state.showLibraryTitleYear) {
                            stringResource(R.string.settings_show_library_title_year_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.showLibraryTitleYear,
                        onClick = { onEvent(SettingsState.Event.ShowLibraryTitleYearToggled) },
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
                    val videoQualityIndex = videoQualitySliderEntries
                        .indexOf(state.preferredVideoQuality).coerceAtLeast(0)
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_preferred_video_quality_title),
                        valueText = videoQualitySliderEntries[videoQualityIndex].label(),
                        value = videoQualityIndex,
                        valueRange = 0..videoQualitySliderEntries.lastIndex,
                        enabled = true,
                        onValueChange = {
                            onEvent(
                                SettingsState.Event.PreferredVideoQualitySelected(
                                    videoQualitySliderEntries[it],
                                ),
                            )
                        },
                    )
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_player_buffer_title),
                        valueText = state.playerBufferProfile.detailsText(),
                        value = PlayerBufferProfile.entries.indexOf(state.playerBufferProfile)
                            .coerceAtLeast(0),
                        valueRange = 0..PlayerBufferProfile.entries.lastIndex,
                        enabled = true,
                        onValueChange = {
                            onEvent(
                                SettingsState.Event.PlayerBufferProfileSelected(
                                    PlayerBufferProfile.entries[it],
                                ),
                            )
                        },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_player_orientation_title),
                        value = state.playerOrientationMode.label(),
                        hint = state.playerOrientationMode.hint(),
                        onClick = { activePicker = SettingsMobilePicker.PLAYER_ORIENTATION },
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
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_mobile_auto_skip_delay_label),
                        valueText = stringResource(
                            R.string.settings_next_episode_switch_delay_seconds,
                            state.autoSkipDelaySeconds,
                        ),
                        value = state.autoSkipDelaySeconds,
                        valueRange = 1..15,
                        enabled = state.autoSkipOpeningsEndings,
                        onValueChange = {
                            onEvent(SettingsState.Event.AutoSkipDelayChanged(it))
                        },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_mobile_show_opening_on_timeline_label),
                        hint = if (state.showOpeningOnTimeline) {
                            stringResource(R.string.settings_mobile_show_opening_on_timeline_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.showOpeningOnTimeline,
                        onClick = { onEvent(SettingsState.Event.ShowOpeningOnTimelineToggled) },
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
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_next_episode_switch_delay_label),
                        valueText = state.nextEpisodeSwitchDelaySeconds.toNextEpisodeSwitchDelayText(),
                        value = state.nextEpisodeSwitchDelaySeconds,
                        valueRange = 0..30,
                        enabled = state.autoPlayNextEpisode,
                        onValueChange = {
                            onEvent(SettingsState.Event.NextEpisodeSwitchDelayChanged(it))
                        },
                    )
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_ask_dubbing_on_watch_label),
                        hint = if (state.askDubbingOnWatch) {
                            stringResource(R.string.settings_ask_dubbing_on_watch_enabled)
                        } else {
                            stringResource(R.string.settings_disabled)
                        },
                        enabled = state.askDubbingOnWatch,
                        onClick = { onEvent(SettingsState.Event.AskDubbingOnWatchToggled) },
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
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_mobile_watched_short_label),
                        valueText = stringResource(
                            R.string.settings_mobile_watched_minutes_value,
                            state.watchedThresholds.shortMinutes,
                        ),
                        value = state.watchedThresholds.shortMinutes,
                        valueRange = WatchedThresholds.SHORT_MINUTES_RANGE,
                        enabled = true,
                        onValueChange = {
                            onEvent(
                                SettingsState.Event.WatchedThresholdsChanged(
                                    state.watchedThresholds.copy(shortMinutes = it),
                                ),
                            )
                        },
                    )
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_mobile_watched_medium_label),
                        valueText = stringResource(
                            R.string.settings_mobile_watched_minutes_value,
                            state.watchedThresholds.mediumMinutes,
                        ),
                        value = state.watchedThresholds.mediumMinutes,
                        valueRange = WatchedThresholds.MEDIUM_MINUTES_RANGE,
                        enabled = true,
                        onValueChange = {
                            onEvent(
                                SettingsState.Event.WatchedThresholdsChanged(
                                    state.watchedThresholds.copy(mediumMinutes = it),
                                ),
                            )
                        },
                    )
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_mobile_watched_long_label),
                        valueText = stringResource(
                            R.string.settings_mobile_watched_minutes_value,
                            state.watchedThresholds.longMinutes,
                        ),
                        value = state.watchedThresholds.longMinutes,
                        valueRange = WatchedThresholds.LONG_MINUTES_RANGE,
                        enabled = true,
                        onValueChange = {
                            onEvent(
                                SettingsState.Event.WatchedThresholdsChanged(
                                    state.watchedThresholds.copy(longMinutes = it),
                                ),
                            )
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
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_player_advanced)) {
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
                    SettingsMobileToggleRow(
                        label = stringResource(R.string.settings_mobile_advanced_volume_label),
                        hint = if (state.advancedPlayerVolumeEnabled) {
                            stringResource(R.string.settings_mobile_advanced_volume_enabled)
                        } else {
                            stringResource(R.string.settings_mobile_advanced_volume_disabled)
                        },
                        enabled = state.advancedPlayerVolumeEnabled,
                        onClick = { onEvent(SettingsState.Event.AdvancedPlayerVolumeToggled) },
                    )
                    if (state.volumeStabilizationSupported) {
                        SettingsMobileToggleRow(
                            label = stringResource(R.string.settings_mobile_volume_stabilization_label),
                            hint = if (state.volumeStabilizationEnabled) {
                                stringResource(R.string.settings_mobile_volume_stabilization_enabled)
                            } else {
                                stringResource(R.string.settings_mobile_volume_stabilization_disabled)
                            },
                            enabled = state.volumeStabilizationEnabled,
                            onClick = { onEvent(SettingsState.Event.VolumeStabilizationToggled) },
                        )
                    }
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
                }
            }

            item {
                SettingsMobileSection(
                    title = stringResource(R.string.settings_tab_player_subtitles),
                    subtitle = stringResource(R.string.settings_subtitle_style_alloha_hint),
                ) {
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_subtitle_size_title),
                        valueText = state.subtitleStyle.textSize.toSubtitlePercentText(),
                        value = state.subtitleStyle.textSize,
                        valueRange = 50..200,
                        enabled = true,
                        onValueChange = {
                            onEvent(
                                SettingsState.Event.SubtitleStyleSelected(
                                    state.subtitleStyle.copy(textSize = it),
                                ),
                            )
                        },
                    )
                    SettingsMobileSliderRow(
                        label = stringResource(R.string.settings_subtitle_offset_title),
                        valueText = state.subtitleStyle.offset.toSubtitlePercentText(),
                        value = state.subtitleStyle.offset,
                        valueRange = 0..20,
                        enabled = true,
                        onValueChange = {
                            onEvent(
                                SettingsState.Event.SubtitleStyleSelected(
                                    state.subtitleStyle.copy(offset = it),
                                ),
                            )
                        },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_subtitle_color_title),
                        value = state.subtitleStyle.textColor.label(),
                        onClick = { activePicker = SettingsMobilePicker.SUBTITLE_COLOR },
                    )
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_subtitle_background_title),
                        value = state.subtitleStyle.background.label(),
                        onClick = { activePicker = SettingsMobilePicker.SUBTITLE_BACKGROUND },
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
                    SettingsMobileOptionRow(
                        label = stringResource(R.string.settings_cache_storage_title),
                        value = Formatter.formatShortFileSize(
                            context,
                            state.cacheStorageTotalBytes,
                        ),
                        hint = stringResource(R.string.settings_cache_storage_button_hint),
                        onClick = {
                            onEvent(SettingsState.Event.CacheStorageRefreshRequested)
                            showCacheStorageDialog = true
                        },
                    )
                }
            }

            item {
                SettingsMobileSection(title = stringResource(R.string.settings_mobile_section_about)) {
                    SettingsMobileAboutRow(
                        label = stringResource(R.string.settings_version_label),
                        hint = BuildConfig.VERSION_NAME,
                    )
                    if (state.isFallbackSessionStorage) {
                        SettingsMobileAboutRow(
                            label = stringResource(R.string.settings_session_storage_label),
                            hint = stringResource(R.string.settings_session_storage_fallback),
                        )
                    }
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
            options = availableAppThemes.map {
                SettingsMobilePickerOption(
                    it,
                    it.label(),
                    it.hint(),
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.AppThemeSelected(it))
                activePicker = null
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
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.BackgroundStyleSelected(it))
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
                    it.hint(),
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
                    it.hint(),
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
                    it.hint(),
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
                    it.hint(),
                )
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PreferredPlayerSelected(it))
                activePicker = null
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
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(SettingsState.Event.PlayerOrientationModeSelected(it))
                activePicker = null
            },
        )

        SettingsMobilePicker.SUBTITLE_COLOR -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_subtitle_color_title),
            selectedValue = state.subtitleStyle.textColor,
            options = PlayerSubtitleTextColor.entries.map {
                SettingsMobilePickerOption(it, it.label(), labelColor = it.color)
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(
                    SettingsState.Event.SubtitleStyleSelected(
                        state.subtitleStyle.copy(textColor = it),
                    ),
                )
                activePicker = null
            },
        )

        SettingsMobilePicker.SUBTITLE_BACKGROUND -> SettingsMobilePickerSheet(
            title = stringResource(R.string.settings_subtitle_background_title),
            selectedValue = state.subtitleStyle.background,
            options = PlayerSubtitleBackground.entries.map {
                SettingsMobilePickerOption(it, it.label())
            },
            onDismiss = { activePicker = null },
            onSelected = {
                onEvent(
                    SettingsState.Event.SubtitleStyleSelected(
                        state.subtitleStyle.copy(background = it),
                    ),
                )
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

    if (showCacheStorageDialog) {
        CacheStorageMobileDialog(
            entries = state.cacheStorageEntries,
            totalBytes = state.cacheStorageTotalBytes,
            isLoading = state.isCacheStorageLoading,
            onRefresh = { onEvent(SettingsState.Event.CacheStorageRefreshRequested) },
            onDismiss = { showCacheStorageDialog = false },
        )
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
