package su.afk.yummy.tv.feature.settings

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.storage.settings.PosterQuality
import su.afk.yummy.tv.core.storage.settings.PreferredPlayer
import su.afk.yummy.tv.core.storage.settings.PreviewCacheSize
import su.afk.yummy.tv.feature.settings.view.QualityRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow

private enum class SettingsTab(@param:StringRes val labelRes: Int) {
    POSTERS(R.string.settings_tab_posters),
    CARDS(R.string.settings_tab_cards),
    PLAYER(R.string.settings_tab_player),
    CACHE(R.string.settings_tab_cache),
    TV_HOME(R.string.settings_tab_tv_home),
    ABOUT(R.string.settings_tab_about),
}

@Composable
fun SettingsTvScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.POSTERS) }
    var contentAnchorTab by remember { mutableStateOf(SettingsTab.POSTERS) }
    val contentFocusRequester = remember { FocusRequester() }
    val tabFocusRequesters = remember {
        SettingsTab.entries.associateWith { FocusRequester() }
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
            SettingsTab.entries.forEach { tab ->
                SettingsTabItem(
                    label = stringResource(tab.labelRes),
                    selected = tab == selectedTab,
                    modifier = Modifier.focusRequester(tabFocusRequesters.getValue(tab)),
                    contentFocusRequester = contentFocusRequester,
                    onSelected = {
                        selectedTab = tab
                        contentAnchorTab = tab
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .focusRequester(contentFocusRequester)
                .focusProperties {
                    up = tabFocusRequesters.getValue(contentAnchorTab)
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionUp) {
                        tabFocusRequesters.getValue(contentAnchorTab).requestFocus()
                        true
                    } else {
                        false
                    }
                }
                .focusGroup(),
            contentAlignment = Alignment.TopCenter,
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "settings_tab_content",
                modifier = Modifier.widthIn(max = 720.dp),
            ) { tab ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    when (tab) {
                        SettingsTab.POSTERS -> PosterQuality.entries.forEachIndexed { index, quality ->
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

                        SettingsTab.CARDS -> ToggleRow(
                            label = stringResource(R.string.settings_show_screenshots_label),
                            hint = stringResource(R.string.settings_show_screenshots_hint),
                            enabled = state.showScreenshotsOnFocus,
                            onClick = { onEvent(SettingsState.Event.ShowScreenshotsOnFocusToggled) },
                        )

                        SettingsTab.PLAYER -> PreferredPlayer.entries.forEachIndexed { index, player ->
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

                        SettingsTab.CACHE -> PreviewCacheSize.entries.forEachIndexed { index, size ->
                            QualityRow(
                                label = size.label(),
                                hint = size.hint(),
                                selected = size == state.previewCacheSize,
                                onClick = { onEvent(SettingsState.Event.PreviewCacheSizeSelected(size)) },
                            )
                            if (index < PreviewCacheSize.entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
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
                                    if (!state.isPreviewChannelBrowsable) onEvent(SettingsState.Event.RequestPreviewChannelBrowsable)
                                },
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
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 8.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                            )
                            AboutRow(
                                label = stringResource(R.string.settings_feedback_label),
                                hint = repositoryUrl,
                                onClick = { uriHandler.openUri(repositoryUrl) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterQuality.label(): String = stringResource(
    when (this) {
        PosterQuality.LOW -> R.string.settings_poster_quality_low
        PosterQuality.STANDARD -> R.string.settings_poster_quality_standard
        PosterQuality.MEGA -> R.string.settings_poster_quality_mega
        PosterQuality.HIGH -> R.string.settings_poster_quality_high
    },
)

@Composable
private fun PosterQuality.hint(): String = stringResource(
    when (this) {
        PosterQuality.LOW -> R.string.settings_poster_quality_low_hint
        PosterQuality.STANDARD -> R.string.settings_poster_quality_standard_hint
        PosterQuality.MEGA -> R.string.settings_poster_quality_mega_hint
        PosterQuality.HIGH -> R.string.settings_poster_quality_high_hint
    },
)

@Composable
private fun PreferredPlayer.label(): String = when (this) {
    PreferredPlayer.NONE -> stringResource(R.string.settings_preferred_player_none)
    PreferredPlayer.KODIK -> stringResource(R.string.settings_preferred_player_kodik)
    PreferredPlayer.AKSOR -> stringResource(R.string.settings_preferred_player_aksor)
    PreferredPlayer.ALLOHA -> stringResource(R.string.settings_preferred_player_alloha)
    PreferredPlayer.CVH -> stringResource(R.string.settings_preferred_player_cvh)
}

@Composable
private fun PreferredPlayer.hint(): String = when (this) {
    PreferredPlayer.NONE -> stringResource(R.string.settings_preferred_player_none_hint)
    PreferredPlayer.KODIK -> stringResource(R.string.settings_preferred_player_kodik_hint)
    PreferredPlayer.AKSOR -> stringResource(R.string.settings_preferred_player_aksor_hint)
    PreferredPlayer.ALLOHA -> stringResource(R.string.settings_preferred_player_alloha_hint)
    PreferredPlayer.CVH -> stringResource(R.string.settings_preferred_player_cvh_hint)
}

@Composable
private fun PreviewCacheSize.label(): String = stringResource(
    when (this) {
        PreviewCacheSize.MB_50 -> R.string.settings_cache_size_min
        PreviewCacheSize.MB_100 -> R.string.settings_cache_size_standard
        PreviewCacheSize.MB_200 -> R.string.settings_cache_size_large
        PreviewCacheSize.MB_300 -> R.string.settings_cache_size_max
    },
)

@Composable
private fun PreviewCacheSize.hint(): String = stringResource(
    when (this) {
        PreviewCacheSize.MB_50 -> R.string.settings_cache_size_50
        PreviewCacheSize.MB_100 -> R.string.settings_cache_size_100
        PreviewCacheSize.MB_200 -> R.string.settings_cache_size_200
        PreviewCacheSize.MB_300 -> R.string.settings_cache_size_300
    },
)

@Composable
private fun AboutRow(
    label: String,
    hint: String,
    onClick: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .background(
                color = if (focused) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f) else Color.Transparent,
                shape = shape,
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = if (onClick != null) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun SettingsTabItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    contentFocusRequester: FocusRequester,
    onSelected: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val contentColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.onBackground
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val indicatorColor = when {
        focused -> MaterialTheme.colorScheme.primary
        selected -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
        else -> Color.Transparent
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .focusProperties {
                down = contentFocusRequester
            }
            .onFocusChanged { if (it.isFocused) onSelected() }
            .background(
                color = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable(interactionSource = interactionSource, indication = null) { onSelected() }
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(24.dp)
                .height(2.dp)
                .background(color = indicatorColor, shape = RoundedCornerShape(1.dp)),
        )
    }
}
