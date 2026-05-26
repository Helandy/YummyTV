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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.storage.settings.AppTheme
import su.afk.yummy.tv.core.storage.settings.DetailsButtonAction
import su.afk.yummy.tv.core.storage.settings.PosterQuality
import su.afk.yummy.tv.core.storage.settings.PreferredPlayer
import su.afk.yummy.tv.core.storage.settings.PreviewCacheSize
import su.afk.yummy.tv.feature.settings.view.QualityRow
import su.afk.yummy.tv.feature.settings.view.ToggleRow

private enum class SettingsTab(@param:StringRes val labelRes: Int) {
    THEME(R.string.settings_tab_theme),
    POSTERS(R.string.settings_tab_posters),
    CARDS(R.string.settings_tab_cards),
    DETAILS(R.string.settings_tab_details),
    PLAYER(R.string.settings_tab_player),
    CACHE(R.string.settings_tab_cache),
    API(R.string.settings_tab_api),
    TV_HOME(R.string.settings_tab_tv_home),
    ABOUT(R.string.settings_tab_about),
}

private data class DetailsButtonOrderItem(
    val key: String,
    val action: DetailsButtonAction,
    val label: String,
)

@Composable
fun SettingsTvScreen(
    state: SettingsState.State,
    effect: Flow<SettingsState.Effect>,
    onEvent: (SettingsState.Event) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(SettingsTab.THEME) }
    var contentAnchorTab by remember { mutableStateOf(SettingsTab.THEME) }
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
                        SettingsTab.THEME -> AppTheme.entries.forEachIndexed { index, theme ->
                            QualityRow(
                                label = theme.label(),
                                hint = theme.hint(),
                                selected = theme == state.appTheme,
                                onClick = { onEvent(SettingsState.Event.AppThemeSelected(theme)) },
                            )
                            if (index < AppTheme.entries.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                                )
                            }
                        }

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

                        SettingsTab.DETAILS -> DetailsButtonOrderContent(
                            order = state.detailsButtonOrder,
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

                        SettingsTab.API -> ApiSettingsContent(
                            token = state.yaniApplicationToken,
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
private fun DetailsButtonOrderContent(
    order: List<DetailsButtonAction>,
    onMoveUp: (DetailsButtonAction) -> Unit,
    onMoveDown: (DetailsButtonAction) -> Unit,
    onReset: () -> Unit,
) {
    val items = order.toDetailsButtonOrderItems()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        DetailsButtonOrderResetRow(onReset = onReset)
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 8.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
        )
        items.forEachIndexed { index, item ->
            key(item.key) {
                DetailsButtonOrderRow(
                    label = item.label,
                    position = index + 1,
                    canMoveUp = index > 0,
                    canMoveDown = index < items.lastIndex,
                    onMoveUp = { onMoveUp(item.action) },
                    onMoveDown = { onMoveDown(item.action) },
                )
            }
            if (index < items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
            }
        }
    }
}

@Composable
private fun DetailsButtonOrderResetRow(onReset: () -> Unit) {
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
            .clickable(interactionSource = interactionSource, indication = null, onClick = onReset)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.RestartAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_details_buttons_reset),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.settings_details_buttons_reset_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun DetailsButtonOrderRow(
    label: String,
    position: Int,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.settings_details_button_position, position),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FocusableIconButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(R.string.settings_details_button_move_up),
            onClick = onMoveUp,
            enabled = canMoveUp,
        )
        FocusableIconButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.settings_details_button_move_down),
            onClick = onMoveDown,
            enabled = canMoveDown,
        )
    }
}

@Composable
private fun FocusableIconButton(
    icon: ImageVector,
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val contentColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.24f)
        focused -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val backgroundColor = when {
        !enabled -> Color.Transparent
        focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .background(backgroundColor, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (enabled) onClick()
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = contentColor,
        )
    }
}

@Composable
private fun ApiSettingsContent(
    token: String,
    onTokenChanged: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_yani_application_token_label),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        OutlinedTextField(
            value = token,
            onValueChange = onTokenChanged,
            placeholder = { Text(stringResource(R.string.settings_yani_application_token_placeholder)) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.settings_yani_application_token_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun List<DetailsButtonAction>.toDetailsButtonOrderItems(): List<DetailsButtonOrderItem> = buildList {
    var index = 0
    while (index <= this@toDetailsButtonOrderItems.lastIndex) {
        val action = this@toDetailsButtonOrderItems[index]
        val nextAction = this@toDetailsButtonOrderItems.getOrNull(index + 1)
        if (action == DetailsButtonAction.LIBRARY && nextAction == DetailsButtonAction.FAVORITE) {
            add(
                DetailsButtonOrderItem(
                    key = "LIBRARY_FAVORITE",
                    action = DetailsButtonAction.LIBRARY,
                    label = stringResource(R.string.settings_details_button_library_favorite),
                ),
            )
            index += 2
        } else if (action != DetailsButtonAction.FAVORITE) {
            add(
                DetailsButtonOrderItem(
                    key = action.name,
                    action = action,
                    label = action.label(),
                ),
            )
            index += 1
        } else {
            index += 1
        }
    }
}

@Composable
private fun AppTheme.label(): String = stringResource(
    when (this) {
        AppTheme.WARM_AMBER -> R.string.settings_theme_warm_amber
        AppTheme.SAKURA -> R.string.settings_theme_sakura
        AppTheme.MINT -> R.string.settings_theme_mint
        AppTheme.OCEAN -> R.string.settings_theme_ocean
        AppTheme.GRAPHITE -> R.string.settings_theme_graphite
    },
)

@Composable
private fun AppTheme.hint(): String = stringResource(
    when (this) {
        AppTheme.WARM_AMBER -> R.string.settings_theme_warm_amber_hint
        AppTheme.SAKURA -> R.string.settings_theme_sakura_hint
        AppTheme.MINT -> R.string.settings_theme_mint_hint
        AppTheme.OCEAN -> R.string.settings_theme_ocean_hint
        AppTheme.GRAPHITE -> R.string.settings_theme_graphite_hint
    },
)

@Composable
private fun DetailsButtonAction.label(): String = stringResource(
    when (this) {
        DetailsButtonAction.WATCH -> R.string.settings_details_button_watch
        DetailsButtonAction.LIBRARY -> R.string.settings_details_button_library
        DetailsButtonAction.FAVORITE -> R.string.settings_details_button_favorite
        DetailsButtonAction.EPISODES -> R.string.settings_details_button_episodes
        DetailsButtonAction.SUBSCRIPTIONS -> R.string.settings_details_button_subscriptions
        DetailsButtonAction.FULL_DETAILS -> R.string.settings_details_button_full_details
        DetailsButtonAction.TRAILERS -> R.string.settings_details_button_trailers
        DetailsButtonAction.SIMILAR -> R.string.settings_details_button_similar
        DetailsButtonAction.VIEWING_ORDER -> R.string.settings_details_button_viewing_order
        DetailsButtonAction.RATING -> R.string.settings_details_button_rating
        DetailsButtonAction.COLLECTIONS -> R.string.settings_details_button_collections
        DetailsButtonAction.SCREENSHOTS -> R.string.settings_details_button_screenshots
    },
)

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
