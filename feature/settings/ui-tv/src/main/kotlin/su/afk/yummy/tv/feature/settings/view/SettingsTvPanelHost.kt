package su.afk.yummy.tv.feature.settings.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.model.settings.BackgroundStyle
import su.afk.yummy.tv.core.model.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.model.settings.PlayerBufferProfile
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleBackground
import su.afk.yummy.tv.core.model.settings.PlayerSubtitleTextColor
import su.afk.yummy.tv.core.model.settings.PosterCardSize
import su.afk.yummy.tv.core.model.settings.PosterQuality
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.settings.WatchedThresholds
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.core.preferences.interface_mode.AppInterfaceMode
import su.afk.yummy.tv.core.utils.system.openExternalUri
import su.afk.yummy.tv.feature.settings.BuildConfig
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.model.SettingsTab
import su.afk.yummy.tv.feature.settings.utils.availableAppThemes
import su.afk.yummy.tv.feature.settings.utils.color
import su.afk.yummy.tv.feature.settings.utils.detailsText
import su.afk.yummy.tv.feature.settings.utils.hint
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.utils.restoreCategoryFocusOnLeft
import su.afk.yummy.tv.feature.settings.utils.toNextEpisodeSwitchDelayText
import su.afk.yummy.tv.feature.settings.utils.toPreviewCacheSizeText
import su.afk.yummy.tv.feature.settings.utils.toSubtitlePercentText
import su.afk.yummy.tv.feature.settings.utils.videoQualitySliderEntries

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun SettingsTvPanelHost(
    state: SettingsState.State,
    selectedTab: SettingsTab,
    tabFocusRequesters: Map<SettingsTab, FocusRequester>,
    contentFocusRequesters: Map<SettingsTab, FocusRequester>,
    onInterfaceModeSelected: (AppInterfaceMode) -> Unit,
    onEvent: (SettingsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Выход фокуса влево из панели всегда ведёт к текущей категории слева
            // (иначе пространственный поиск с нижних строк уводит в соседнюю категорию).
            .focusProperties {
                onExit = {
                    if (requestedFocusDirection == FocusDirection.Left) {
                        tabFocusRequesters.getValue(selectedTab).requestFocus()
                    }
                }
            }
            .focusGroup(),
        contentAlignment = Alignment.TopStart,
    ) {
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
            label = "settings_tab_content",
            modifier = Modifier.widthIn(max = 720.dp),
        ) { tab ->
            val tabFocusRequester = tabFocusRequesters.getValue(tab)
            val tabContentFocusRequester = contentFocusRequesters.getValue(tab)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                when (tab) {
                    SettingsTab.INTERFACE -> AppInterfaceMode.entries.forEachIndexed { index, mode ->
                        QualityRow(
                            label = mode.label(),
                            hint = mode.hint(),
                            selected = mode == state.interfaceMode,
                            onClick = { onInterfaceModeSelected(mode) },
                            modifier = Modifier
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(tabContentFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                                .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                        )
                        if (index < AppInterfaceMode.entries.lastIndex) {
                            SettingsDivider()
                        }
                    }

                    SettingsTab.SEARCH -> {
                        ToggleRow(
                            label = stringResource(R.string.settings_save_last_search_label),
                            hint = if (state.saveLastSearchEnabled) {
                                stringResource(R.string.settings_save_last_search_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.saveLastSearchEnabled,
                            onClick = { onEvent(SettingsState.Event.SaveLastSearchToggled) },
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(tabFocusRequester),
                        )
                    }

                    SettingsTab.THEME -> availableAppThemes.forEachIndexed { index, theme ->
                        QualityRow(
                            label = theme.label(),
                            hint = theme.hint(),
                            selected = theme == state.appTheme,
                            onClick = { onEvent(SettingsState.Event.AppThemeSelected(theme)) },
                            modifier = Modifier
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(tabContentFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                                .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                        )
                        if (index < availableAppThemes.lastIndex) {
                            SettingsDivider()
                        }
                    }

                    SettingsTab.BACKGROUND -> BackgroundStyle.entries.forEachIndexed { index, style ->
                        QualityRow(
                            label = style.label(),
                            hint = style.hint(),
                            selected = style == state.backgroundStyle,
                            onClick = {
                                onEvent(SettingsState.Event.BackgroundStyleSelected(style))
                            },
                            modifier = Modifier
                                .then(
                                    if (index == 0) {
                                        Modifier.focusRequester(tabContentFocusRequester)
                                    } else {
                                        Modifier
                                    },
                                )
                                .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                        )
                        if (index < BackgroundStyle.entries.lastIndex) {
                            SettingsDivider()
                        }
                    }

                    SettingsTab.POSTER_SIZE -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_poster_size_title))
                        PosterCardSize.entries.forEachIndexed { index, size ->
                            QualityRow(
                                label = size.label(),
                                hint = size.hint(),
                                selected = size == state.posterCardSize,
                                onClick = {
                                    onEvent(
                                        SettingsState.Event.PosterCardSizeSelected(
                                            size
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(tabContentFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                            )
                            if (index < PosterCardSize.entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }

                    SettingsTab.POSTERS -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_poster_quality_title))
                        PosterQuality.entries.forEachIndexed { index, quality ->
                            QualityRow(
                                label = quality.label(),
                                hint = quality.hint(),
                                selected = quality == state.posterQuality,
                                onClick = {
                                    onEvent(
                                        SettingsState.Event.PosterQualitySelected(
                                            quality,
                                        ),
                                    )
                                },
                                modifier = Modifier
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(tabContentFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                            )
                            if (index < PosterQuality.entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }

                    SettingsTab.TOP_TITLE_YEAR -> {
                        ToggleRow(
                            label = stringResource(R.string.settings_show_top_title_year),
                            hint = if (state.showTopTitleYear) {
                                stringResource(R.string.settings_show_top_title_year_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.showTopTitleYear,
                            onClick = { onEvent(SettingsState.Event.ShowTopTitleYearToggled) },
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(tabFocusRequester),
                        )
                    }

                    SettingsTab.LIBRARY_TITLE_YEAR -> {
                        ToggleRow(
                            label = stringResource(R.string.settings_show_library_title_year),
                            hint = if (state.showLibraryTitleYear) {
                                stringResource(R.string.settings_show_library_title_year_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.showLibraryTitleYear,
                            onClick = { onEvent(SettingsState.Event.ShowLibraryTitleYearToggled) },
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(tabFocusRequester),
                        )
                    }

                    SettingsTab.CONTINUE_WATCHING -> {
                        SettingsSectionTitle(
                            text = stringResource(
                                R.string.settings_library_continue_watching_card_size_title,
                            ),
                        )
                        LibraryContinueWatchingCardSize.entries.forEachIndexed { index, size ->
                            QualityRow(
                                label = size.label(),
                                hint = size.hint(),
                                selected = size == state.libraryContinueWatchingCardSize,
                                onClick = {
                                    onEvent(
                                        SettingsState.Event.LibraryContinueWatchingCardSizeSelected(
                                            size,
                                        ),
                                    )
                                },
                                modifier = Modifier
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(tabContentFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                            )
                            if (index < LibraryContinueWatchingCardSize.entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }

                    SettingsTab.DETAILS -> DetailsButtonOrderPanel(
                        order = state.detailsButtonOrder,
                        upFocusRequester = tabFocusRequester,
                        contentFocusRequester = tabContentFocusRequester,
                        onMoveUp = {
                            onEvent(
                                SettingsState.Event.DetailsButtonMoved(
                                    action = it,
                                    direction = DetailsButtonMoveDirection.UP,
                                ),
                            )
                        },
                        onMoveDown = {
                            onEvent(
                                SettingsState.Event.DetailsButtonMoved(
                                    action = it,
                                    direction = DetailsButtonMoveDirection.DOWN,
                                ),
                            )
                        },
                        onReset = { onEvent(SettingsState.Event.DetailsButtonOrderReset) },
                    )

                    SettingsTab.PLAYER_QUALITY -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_preferred_video_quality_title))
                        val qualities = videoQualitySliderEntries
                        val qualityIndex =
                            qualities.indexOf(state.preferredVideoQuality).coerceAtLeast(0)
                        SettingsSliderRow(
                            label = stringResource(R.string.settings_preferred_video_quality_title),
                            valueText = qualities[qualityIndex].label(),
                            value = qualityIndex,
                            valueRange = 0..qualities.lastIndex,
                            enabled = true,
                            onValueChange = {
                                onEvent(
                                    SettingsState.Event.PreferredVideoQualitySelected(qualities[it]),
                                )
                            },
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(
                                    tabFocusRequester,
                                    alsoOnVertical = true
                                ),
                        )
                    }

                    SettingsTab.PLAYER_BUFFER -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_player_buffer_title))
                        val profiles = PlayerBufferProfile.entries
                        SettingsSliderRow(
                            label = stringResource(R.string.settings_player_buffer_title),
                            valueText = state.playerBufferProfile.detailsText(),
                            value = profiles.indexOf(state.playerBufferProfile).coerceAtLeast(0),
                            valueRange = 0..profiles.lastIndex,
                            enabled = true,
                            onValueChange = {
                                onEvent(
                                    SettingsState.Event.PlayerBufferProfileSelected(profiles[it]),
                                )
                            },
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(
                                    tabFocusRequester,
                                    alsoOnVertical = true
                                ),
                        )
                    }

                    SettingsTab.PLAYER_SUBTITLES -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_subtitle_style_title))
                        Text(
                            text = stringResource(R.string.settings_subtitle_style_alloha_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        )
                        // Первая группа держит фокус входа в категорию, остальные — обычные ряды.
                        SettingsSectionTitle(text = stringResource(R.string.settings_subtitle_size_title))
                        SettingsSliderRow(
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
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(tabFocusRequester),
                        )
                        SettingsSectionTitle(text = stringResource(R.string.settings_subtitle_offset_title))
                        SettingsSliderRow(
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
                        SubtitleStyleGroup(
                            title = stringResource(R.string.settings_subtitle_color_title),
                            values = PlayerSubtitleTextColor.entries,
                            selected = state.subtitleStyle.textColor,
                            labelOf = { it.label() },
                            colorOf = { it.color },
                            tabFocusRequester = tabFocusRequester,
                            onSelected = {
                                onEvent(
                                    SettingsState.Event.SubtitleStyleSelected(
                                        state.subtitleStyle.copy(textColor = it),
                                    ),
                                )
                            },
                        )
                        SubtitleStyleGroup(
                            title = stringResource(R.string.settings_subtitle_background_title),
                            values = PlayerSubtitleBackground.entries,
                            selected = state.subtitleStyle.background,
                            labelOf = { it.label() },
                            tabFocusRequester = tabFocusRequester,
                            onSelected = {
                                onEvent(
                                    SettingsState.Event.SubtitleStyleSelected(
                                        state.subtitleStyle.copy(background = it),
                                    ),
                                )
                            },
                        )
                    }

                    SettingsTab.PLAYER -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_player_playback_title))
                        ToggleRow(
                            label = stringResource(R.string.settings_auto_skip_label),
                            hint = if (state.autoSkipOpeningsEndings) {
                                stringResource(R.string.settings_auto_skip_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.autoSkipOpeningsEndings,
                            onClick = { onEvent(SettingsState.Event.AutoSkipOpeningsEndingsToggled) },
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(tabFocusRequester),
                        )
                        SettingsDivider()
                        SettingsSliderRow(
                            label = stringResource(R.string.settings_tv_auto_skip_delay_label),
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
                        SettingsDivider()
                        ToggleRow(
                            label = stringResource(R.string.settings_tv_show_opening_on_timeline_label),
                            hint = if (state.showOpeningOnTimeline) {
                                stringResource(R.string.settings_tv_show_opening_on_timeline_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.showOpeningOnTimeline,
                            onClick = { onEvent(SettingsState.Event.ShowOpeningOnTimelineToggled) },
                        )
                        SettingsDivider()
                        ToggleRow(
                            label = stringResource(R.string.settings_auto_play_next_episode_label),
                            hint = if (state.autoPlayNextEpisode) {
                                stringResource(R.string.settings_auto_play_next_episode_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.autoPlayNextEpisode,
                            onClick = {
                                onEvent(SettingsState.Event.AutoPlayNextEpisodeToggled)
                            },
                        )
                        SettingsDivider()
                        SettingsSliderRow(
                            label = stringResource(R.string.settings_next_episode_switch_delay_label),
                            valueText = state.nextEpisodeSwitchDelaySeconds.toNextEpisodeSwitchDelayText(),
                            value = state.nextEpisodeSwitchDelaySeconds,
                            valueRange = 0..30,
                            enabled = state.autoPlayNextEpisode,
                            onValueChange = {
                                onEvent(SettingsState.Event.NextEpisodeSwitchDelayChanged(it))
                            },
                        )
                        SettingsDivider()
                        ToggleRow(
                            label = stringResource(R.string.settings_ask_dubbing_on_watch_label),
                            hint = if (state.askDubbingOnWatch) {
                                stringResource(R.string.settings_ask_dubbing_on_watch_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.askDubbingOnWatch,
                            onClick = {
                                onEvent(SettingsState.Event.AskDubbingOnWatchToggled)
                            },
                        )
                        SettingsDivider()
                        ToggleRow(
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
                        SettingsDivider()
                        SettingsSliderRow(
                            label = stringResource(R.string.settings_tv_watched_short_label),
                            valueText = stringResource(
                                R.string.settings_tv_watched_minutes_value,
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
                        SettingsDivider()
                        SettingsSliderRow(
                            label = stringResource(R.string.settings_tv_watched_medium_label),
                            valueText = stringResource(
                                R.string.settings_tv_watched_minutes_value,
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
                        SettingsDivider()
                        SettingsSliderRow(
                            label = stringResource(R.string.settings_tv_watched_long_label),
                            valueText = stringResource(
                                R.string.settings_tv_watched_minutes_value,
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
                        SettingsDivider()
                        ToggleRow(
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
                        SettingsDivider()
                        ToggleRow(
                            label = stringResource(R.string.settings_tv_advanced_volume_label),
                            hint = if (state.advancedPlayerVolumeEnabled) {
                                stringResource(R.string.settings_tv_advanced_volume_enabled)
                            } else {
                                stringResource(R.string.settings_tv_advanced_volume_disabled)
                            },
                            enabled = state.advancedPlayerVolumeEnabled,
                            onClick = {
                                onEvent(SettingsState.Event.AdvancedPlayerVolumeToggled)
                            },
                        )
                        if (state.volumeStabilizationSupported) {
                            SettingsDivider()
                            ToggleRow(
                                label = stringResource(R.string.settings_tv_volume_stabilization_label),
                                hint = if (state.volumeStabilizationEnabled) {
                                    stringResource(R.string.settings_tv_volume_stabilization_enabled)
                                } else {
                                    stringResource(R.string.settings_tv_volume_stabilization_disabled)
                                },
                                enabled = state.volumeStabilizationEnabled,
                                onClick = {
                                    onEvent(SettingsState.Event.VolumeStabilizationToggled)
                                },
                            )
                        }
                        if (state.tvPlayerControlsTutorialDismissed) {
                            SettingsDivider()
                            DetailsButtonOrderResetRow(
                                label = stringResource(R.string.settings_tv_controls_tutorial_reset),
                                hint = stringResource(R.string.settings_tv_controls_tutorial_reset_hint),
                                onReset = {
                                    onEvent(SettingsState.Event.TvPlayerControlsTutorialReset)
                                },
                            )
                        }
                    }

                    SettingsTab.PLAYER_SOURCE -> {
                        PreferredPlayer.entries.forEachIndexed { index, player ->
                            QualityRow(
                                label = player.label(),
                                hint = player.hint(),
                                selected = player == state.preferredPlayer,
                                onClick = {
                                    onEvent(
                                        SettingsState.Event.PreferredPlayerSelected(
                                            player
                                        )
                                    )
                                },
                                modifier = Modifier
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(tabContentFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                            )
                            if (index < PreferredPlayer.entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }

                    SettingsTab.CACHE -> {
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
                                .restoreCategoryFocusOnLeft(
                                    tabFocusRequester,
                                    alsoOnVertical = true
                                ),
                        )
                    }

                    SettingsTab.LANGUAGE -> {
                        YaniContentLanguage.entries.forEachIndexed { index, language ->
                            QualityRow(
                                label = language.label(),
                                selected = language == state.contentLanguage,
                                onClick = {
                                    onEvent(SettingsState.Event.ContentLanguageSelected(language))
                                },
                                modifier = Modifier
                                    .then(
                                        if (index == 0) {
                                            Modifier.focusRequester(tabContentFocusRequester)
                                        } else {
                                            Modifier
                                        },
                                    )
                                    .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
                            )
                            SettingsDivider()
                        }
                    }

                    SettingsTab.API -> {
                        var showCacheStorageDialog by remember { mutableStateOf(false) }
                        ApiSettingsPanel(
                            token = state.yaniApplicationToken,
                            upFocusRequester = tabFocusRequester,
                            contentFocusRequester = tabContentFocusRequester,
                            cacheStorageSize = state.cacheStorageTotalBytes,
                            onTokenChanged = {
                                onEvent(
                                    SettingsState.Event.YaniApplicationTokenChanged(
                                        it
                                    )
                                )
                            },
                            onShowCacheStorage = {
                                onEvent(SettingsState.Event.CacheStorageRefreshRequested)
                                showCacheStorageDialog = true
                            },
                        )
                        if (showCacheStorageDialog) {
                            CacheStorageTvDialog(
                                entries = state.cacheStorageEntries,
                                totalBytes = state.cacheStorageTotalBytes,
                                isLoading = state.isCacheStorageLoading,
                                onRefresh = {
                                    onEvent(SettingsState.Event.CacheStorageRefreshRequested)
                                },
                                onDismiss = { showCacheStorageDialog = false },
                            )
                        }
                    }

                    SettingsTab.TV_HOME -> {
                        ToggleRow(
                            label = stringResource(R.string.settings_preview_channel_label),
                            hint = if (state.isPreviewChannelBrowsable) {
                                stringResource(R.string.settings_preview_channel_added)
                            } else {
                                stringResource(R.string.settings_preview_channel_add_hint)
                            },
                            enabled = state.isPreviewChannelBrowsable,
                            onClick = {
                                if (!state.isPreviewChannelBrowsable) {
                                    onEvent(SettingsState.Event.RequestPreviewChannelBrowsable)
                                }
                            },
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(tabFocusRequester),
                        )
                        SettingsDivider()
                        ToggleRow(
                            label = stringResource(R.string.settings_watch_next_label),
                            hint = if (state.watchNextEnabled) {
                                stringResource(R.string.settings_watch_next_enabled)
                            } else {
                                stringResource(R.string.settings_disabled)
                            },
                            enabled = state.watchNextEnabled,
                            onClick = { onEvent(SettingsState.Event.WatchNextToggled) },
                        )
                    }

                    SettingsTab.ABOUT -> {
                        val repositoryUrl = stringResource(R.string.settings_repository_url)
                        val context = LocalContext.current

                        AboutRow(
                            label = stringResource(R.string.settings_version_label),
                            hint = BuildConfig.VERSION_NAME,
                            modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
                        )
                        if (state.isFallbackSessionStorage) {
                            SettingsDivider()
                            AboutRow(
                                label = stringResource(R.string.settings_session_storage_label),
                                hint = stringResource(R.string.settings_session_storage_fallback),
                                modifier = Modifier.restoreCategoryFocusOnLeft(tabFocusRequester),
                            )
                        }
                        SettingsDivider()
                        AboutRow(
                            label = stringResource(R.string.settings_feedback_label),
                            hint = repositoryUrl,
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreCategoryFocusOnLeft(tabFocusRequester),
                            onClick = { context.openExternalUri(repositoryUrl) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
    )
}

/**
 * Одна группа оформления субтитров: заголовок и радио-ряды вариантов.
 * [firstRowFocusRequester] передают только первой группе категории — она принимает фокус
 * при входе в категорию; «влево» с первого ряда любой группы возвращает в список категорий.
 */
@Composable
private fun <T> SubtitleStyleGroup(
    title: String,
    values: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    tabFocusRequester: FocusRequester,
    onSelected: (T) -> Unit,
    hintOf: (@Composable (T) -> String)? = null,
    firstRowFocusRequester: FocusRequester? = null,
    colorOf: (@Composable (T) -> Color)? = null,
) {
    SettingsSectionTitle(text = title)
    values.forEachIndexed { index, value ->
        QualityRow(
            label = labelOf(value),
            hint = hintOf?.invoke(value).orEmpty(),
            selected = value == selected,
            onClick = { onSelected(value) },
            labelColor = colorOf?.invoke(value),
            modifier = Modifier
                .then(
                    if (index == 0 && firstRowFocusRequester != null) {
                        Modifier.focusRequester(firstRowFocusRequester)
                    } else {
                        Modifier
                    },
                )
                .restoreCategoryFocusOnLeft(tabFocusRequester, index == 0),
        )
        if (index < values.lastIndex) {
            SettingsDivider()
        }
    }
}
