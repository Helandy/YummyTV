package su.afk.yummy.tv.feature.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.preferences.settings.AppTheme
import su.afk.yummy.tv.core.preferences.settings.PosterCardSize
import su.afk.yummy.tv.core.preferences.settings.PosterQuality
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.PreviewCacheSize
import su.afk.yummy.tv.feature.settings.model.SettingsTab
import su.afk.yummy.tv.feature.settings.utils.hint
import su.afk.yummy.tv.feature.settings.utils.label
import su.afk.yummy.tv.feature.settings.utils.restoreTabFocusOnUp
import su.afk.yummy.tv.feature.settings.view.AboutRow
import su.afk.yummy.tv.feature.settings.view.ApiSettingsPanel
import su.afk.yummy.tv.feature.settings.view.DetailsButtonOrderPanel
import su.afk.yummy.tv.feature.settings.view.QualityRow
import su.afk.yummy.tv.feature.settings.view.SettingsTabItem
import su.afk.yummy.tv.feature.settings.view.ToggleRow

@Composable
fun SettingsTvScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.THEME) }
    val contentFocusRequesters = remember {
        SettingsTab.entries.associateWith { FocusRequester() }
    }
    val tabFocusRequesters = remember {
        SettingsTab.entries.associateWith { FocusRequester() }
    }
    val selectedTabFocusRequester = tabFocusRequesters.getValue(selectedTab)
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current

    DisposableEffect(selectedTabFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(selectedTabFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = TvScreenPadding.Horizontal, vertical = TvScreenPadding.Vertical),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusGroup()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SettingsTab.entries.forEachIndexed { index, tab ->
                val tabContentFocusRequester = contentFocusRequesters.getValue(tab)
                SettingsTabItem(
                    label = stringResource(tab.labelRes),
                    selected = tab == selectedTab,
                    modifier = Modifier.focusRequester(tabFocusRequesters.getValue(tab)),
                    contentFocusRequester = tabContentFocusRequester,
                    leftFocusRequester = tabFocusRequesters[SettingsTab.entries.getOrNull(index - 1)]
                        ?: mainMenuFocusRequester.takeIf { index == 0 },
                    rightFocusRequester = tabFocusRequesters[SettingsTab.entries.getOrNull(index + 1)]
                        ?: tabContentFocusRequester,
                    onSelected = {
                        selectedTab = tab
                    },
                    onActivated = {
                        selectedTab = tab
                        runCatching { tabContentFocusRequester.requestFocus() }
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
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
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                )
                            }
                        }

                        SettingsTab.POSTERS -> {
                            Text(
                                text = stringResource(R.string.settings_poster_size_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )
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
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                )
                            }
                            Text(
                                text = stringResource(R.string.settings_poster_quality_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                            PosterQuality.entries.forEachIndexed { index, quality ->
                                QualityRow(
                                    label = quality.label(),
                                    hint = quality.hint(),
                                    selected = quality == state.posterQuality,
                                    onClick = { onEvent(SettingsState.Event.PosterQualitySelected(quality)) },
                                )
                                if (index < PosterQuality.entries.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    )
                                }
                            }
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )
                            ToggleRow(
                                label = stringResource(R.string.settings_show_screenshots_label),
                                hint = stringResource(R.string.settings_show_screenshots_hint),
                                enabled = state.showScreenshotsOnFocus,
                                onClick = { onEvent(SettingsState.Event.ShowScreenshotsOnFocusToggled) },
                            )
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
                                    .restoreTabFocusOnUp(tabFocusRequester),
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )
                            PreferredPlayer.entries.forEachIndexed { index, player ->
                                QualityRow(
                                    label = player.label(),
                                    hint = player.hint(),
                                    selected = player == state.preferredPlayer,
                                    onClick = { onEvent(SettingsState.Event.PreferredPlayerSelected(player)) },
                                )
                                if (index < PreferredPlayer.entries.lastIndex) {
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    )
                                }
                            }
                        }

                        SettingsTab.CACHE -> {
                            Text(
                                text = stringResource(R.string.settings_poster_cache_size_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onBackground,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            )
                            PreviewCacheSize.entries.forEachIndexed { index, size ->
                                QualityRow(
                                    label = size.label(),
                                    hint = size.hint(),
                                    selected = size == state.previewCacheSize,
                                    onClick = { onEvent(SettingsState.Event.PreviewCacheSizeSelected(size)) },
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
                                    HorizontalDivider(
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                    )
                                }
                            }
                        }

                        SettingsTab.API -> ApiSettingsPanel(
                            token = state.yaniApplicationToken,
                            upFocusRequester = tabFocusRequester,
                            contentFocusRequester = tabContentFocusRequester,
                            onTokenChanged = { onEvent(SettingsState.Event.YaniApplicationTokenChanged(it)) },
                        )

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
                                    if (!state.isPreviewChannelBrowsable) onEvent(SettingsState.Event.RequestPreviewChannelBrowsable)
                                },
                                modifier = Modifier
                                    .focusRequester(tabContentFocusRequester)
                                    .restoreTabFocusOnUp(tabFocusRequester),
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )
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
                            val uriHandler = LocalUriHandler.current

                            AboutRow(
                                label = stringResource(R.string.settings_version_label),
                                hint = BuildConfig.VERSION_NAME,
                                modifier = Modifier.restoreTabFocusOnUp(tabFocusRequester),
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )
                            AboutRow(
                                label = stringResource(R.string.settings_feedback_label),
                                hint = repositoryUrl,
                                modifier = Modifier
                                    .focusRequester(tabContentFocusRequester)
                                    .restoreTabFocusOnUp(tabFocusRequester),
                                onClick = { uriHandler.openUri(repositoryUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}
