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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.core.preferences.settings.YaniContentLanguage
import su.afk.yummy.tv.core.utils.openExternalUri
import su.afk.yummy.tv.feature.settings.BuildConfig
import su.afk.yummy.tv.feature.settings.DetailsButtonMoveDirection
import su.afk.yummy.tv.feature.settings.R
import su.afk.yummy.tv.feature.settings.SettingsState
import su.afk.yummy.tv.feature.settings.model.SettingsTab
import su.afk.yummy.tv.feature.settings.utils.hint
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.utils.restoreTabFocusOnUp

@Composable
internal fun SettingsTvPanelHost(
    state: SettingsState.State,
    selectedTab: SettingsTab,
    tabFocusRequesters: Map<SettingsTab, FocusRequester>,
    contentFocusRequesters: Map<SettingsTab, FocusRequester>,
    onEvent: (SettingsState.Event) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup(),
        contentAlignment = Alignment.TopCenter,
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
                    SettingsTab.THEME -> AppTheme.entries.forEachIndexed { index, theme ->
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
                                .restoreTabFocusOnUp(tabFocusRequester, index == 0),
                        )
                        if (index < AppTheme.entries.lastIndex) {
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
                                    .restoreTabFocusOnUp(tabFocusRequester, index == 0),
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
                                    .restoreTabFocusOnUp(tabFocusRequester, index == 0),
                            )
                            if (index < PosterQuality.entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
                        SettingsDivider()
                        ToggleRow(
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
                                    .restoreTabFocusOnUp(tabFocusRequester, index == 0),
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

                    SettingsTab.PLAYER -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_preferred_video_quality_title))
                        PreferredVideoQuality.entries.forEachIndexed { index, quality ->
                            QualityRow(
                                label = quality.label(),
                                hint = quality.hint(),
                                selected = quality == state.preferredVideoQuality,
                                onClick = {
                                    onEvent(
                                        SettingsState.Event.PreferredVideoQualitySelected(
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
                                    .restoreTabFocusOnUp(tabFocusRequester, index == 0),
                            )
                            SettingsDivider()
                        }
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
                        SettingsSectionTitle(
                            text = stringResource(R.string.settings_preferred_player_title),
                        )
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
                            )
                            if (index < PreferredPlayer.entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
                    }

                    SettingsTab.CACHE -> {
                        SettingsSectionTitle(text = stringResource(R.string.settings_poster_cache_size_title))
                        PreviewCacheSize.entries.forEachIndexed { index, size ->
                            QualityRow(
                                label = size.label(),
                                hint = size.hint(),
                                selected = size == state.previewCacheSize,
                                onClick = {
                                    onEvent(
                                        SettingsState.Event.PreviewCacheSizeSelected(
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
                                    .restoreTabFocusOnUp(tabFocusRequester, index == 0),
                            )
                            if (index < PreviewCacheSize.entries.lastIndex) {
                                SettingsDivider()
                            }
                        }
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
                                    .restoreTabFocusOnUp(tabFocusRequester, index == 0),
                            )
                            SettingsDivider()
                        }
                    }

                    SettingsTab.API -> {
                        ApiSettingsPanel(
                            token = state.yaniApplicationToken,
                            upFocusRequester = tabFocusRequester,
                            contentFocusRequester = tabContentFocusRequester,
                            onTokenChanged = {
                                onEvent(
                                    SettingsState.Event.YaniApplicationTokenChanged(
                                        it
                                    )
                                )
                            },
                        )
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
                                .restoreTabFocusOnUp(tabFocusRequester),
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
                            modifier = Modifier.restoreTabFocusOnUp(tabFocusRequester),
                        )
                        SettingsDivider()
                        AboutRow(
                            label = stringResource(R.string.settings_feedback_label),
                            hint = repositoryUrl,
                            modifier = Modifier
                                .focusRequester(tabContentFocusRequester)
                                .restoreTabFocusOnUp(tabFocusRequester),
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
