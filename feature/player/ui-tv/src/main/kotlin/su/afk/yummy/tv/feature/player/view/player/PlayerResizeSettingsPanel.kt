package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.preferences.settings.PlayerResizeMode
import su.afk.yummy.tv.core.preferences.settings.PlayerZoomLevel
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.feature.player.utils.tvResizeLabel
import su.afk.yummy.tv.feature.player.utils.tvResizeMeta
import su.afk.yummy.tv.feature.player.utils.tvZoomLevelLabel

@Composable
internal fun PlayerResizeSettingsPanel(
    visible: Boolean,
    resizeModes: List<PlayerResizeMode>,
    selectedResizeMode: PlayerResizeMode,
    zoomLevels: List<PlayerZoomLevel>,
    selectedZoomLevel: PlayerZoomLevel,
    selectedResizeFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onResizeModeSelected: (PlayerResizeMode) -> Unit,
    onZoomLevelSelected: (PlayerZoomLevel) -> Unit,
    onExitDown: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.7f).dp
        val zoomFocusRequesters = remember(zoomLevels) { zoomLevels.map { FocusRequester() } }
        var pendingZoomFocusIndex by remember { mutableStateOf<Int?>(null) }

        LaunchedEffect(visible) {
            if (visible) {
                runCatching { selectedResizeFocusRequester.requestFocus() }
            }
        }

        LaunchedEffect(visible, selectedResizeMode, selectedZoomLevel, pendingZoomFocusIndex) {
            val index = pendingZoomFocusIndex ?: return@LaunchedEffect
            if (visible && selectedResizeMode == PlayerResizeMode.ZOOM) {
                withFrameNanos { }
                runCatching { zoomFocusRequesters[index].requestFocus() }
                pendingZoomFocusIndex = null
            }
        }

        Column(
            modifier = Modifier
                .width(336.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE6121214))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .heightIn(max = maxPanelHeight)
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.player_resize_title),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                resizeModes.forEach { mode ->
                    PlayerResizeSelectionItem(
                        label = mode.tvResizeLabel(),
                        meta = if (mode == PlayerResizeMode.FIT) "" else mode.tvResizeMeta(),
                        selected = mode == selectedResizeMode,
                        modifier = if (mode == selectedResizeMode) {
                            Modifier.focusRequester(selectedResizeFocusRequester)
                        } else {
                            Modifier
                        },
                        onClick = { onResizeModeSelected(mode) },
                    )
                }
            }
            Text(
                text = stringResource(R.string.player_zoom_level_title),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                zoomLevels.forEachIndexed { index, level ->
                    val previousZoomFocusRequester = zoomFocusRequesters.getOrNull(index - 1)
                    val nextZoomFocusRequester = zoomFocusRequesters.getOrNull(index + 1)
                    PlayerZoomLevelItem(
                        label = level.tvZoomLevelLabel(),
                        selected = selectedResizeMode == PlayerResizeMode.ZOOM && level == selectedZoomLevel,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(zoomFocusRequesters[index])
                            .focusProperties {
                                left = previousZoomFocusRequester ?: FocusRequester.Cancel
                                right = nextZoomFocusRequester ?: FocusRequester.Cancel
                            },
                        onExitDown = onExitDown,
                        onClick = {
                            pendingZoomFocusIndex = index
                            onZoomLevelSelected(level)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerResizeSelectionItem(
    label: String,
    meta: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = when {
            focused -> Color.White
            selected -> Color.White.copy(alpha = 0.86f)
            else -> Color.White.copy(alpha = 0.10f)
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "resize_item_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "resize_item_border",
    )
    val contentColor = if (focused || selected) Color.Black else Color.White

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(2.dp, borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (meta.isNotBlank()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.62f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.Black.copy(alpha = 0.10f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.player_selected),
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor.copy(alpha = 0.72f),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun PlayerZoomLevelItem(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onExitDown: () -> Unit,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val background by animateColorAsState(
        targetValue = when {
            focused -> Color.White
            selected -> Color.White.copy(alpha = 0.86f)
            else -> Color.White.copy(alpha = 0.10f)
        },
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "zoom_level_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "zoom_level_border",
    )
    val contentColor = if (focused || selected) Color.Black else Color.White

    Box(
        modifier = modifier
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false

                when (event.key) {
                    Key.DirectionDown -> {
                        onExitDown()
                        true
                    }

                    else -> false
                }
            }
            .clip(shape)
            .background(background)
            .border(2.dp, borderColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
