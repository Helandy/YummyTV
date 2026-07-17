package su.afk.yummy.tv.feature.details.view.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import su.afk.yummy.tv.core.designsystem.presenter.components.TvOverlayAppear
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.BalancerOption
import su.afk.yummy.tv.feature.details.details.BalancerPickerState

@Composable
internal fun BalancerPickerOverlay(
    picker: BalancerPickerState,
    onConfirmed: (BalancerOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val firstFocusRequester = remember { FocusRequester() }
    val supportedIndices = remember(picker.options) {
        picker.options.mapIndexedNotNull { index, option -> index.takeIf { option.isSupported } }
    }
    val firstSupportedIdx = supportedIndices.firstOrNull() ?: -1
    val lastSupportedIdx = supportedIndices.lastOrNull() ?: -1
    var focusedOptionIndex by remember(picker.options) { mutableIntStateOf(firstSupportedIdx) }

    LaunchedEffect(picker, firstSupportedIdx) {
        if (firstSupportedIdx < 0) return@LaunchedEffect
        focusManager.clearFocus(force = true)
        withFrameNanos { }
        runCatching { firstFocusRequester.requestFocus() }
        withFrameNanos { }
        runCatching { firstFocusRequester.requestFocus() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.82f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.Center,
        ) {
            TvOverlayAppear {
                Column(
                    modifier = Modifier
                        .wrapContentSize()
                        .width(440.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xF21B1B1F))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                        .focusGroup()
                        .onPreviewKeyEvent { event ->
                            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                            when (event.key) {
                                Key.Back, Key.Escape -> {
                                    onDismiss()
                                    true
                                }

                                Key.DirectionUp -> focusedOptionIndex == firstSupportedIdx
                                Key.DirectionDown -> focusedOptionIndex == lastSupportedIdx
                                else -> false
                            }
                        }
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(
                            R.string.details_balancer_title,
                            picker.episodeNumber
                        ),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.70f),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                    if (picker.preferredPlayerUnavailable) {
                        Text(
                            text = stringResource(R.string.details_balancer_preferred_unavailable),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.50f),
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    }
                    picker.options.forEachIndexed { idx, option ->
                        BalancerOptionItem(
                            label = option.playerName.removePrefix(stringResource(R.string.details_player_prefix)),
                            dubbing = option.video.dubbing,
                            views = option.video.views,
                            focusRequester = if (idx == firstSupportedIdx) firstFocusRequester else null,
                            isSupported = option.isSupported,
                            onFocused = { focusedOptionIndex = idx },
                            onClick = { onConfirmed(option) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BalancerOptionItem(
    label: String,
    dubbing: String,
    views: Int?,
    focusRequester: FocusRequester?,
    isSupported: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    if (isSupported) {
        val bgColor by animateColorAsState(
            targetValue = if (focused) Color.White else Color.White.copy(alpha = 0.10f),
            animationSpec = tween(durationMillis = 150),
            label = "balancerItemBg",
        )
        val textColor = if (focused) Color.Black else Color.White
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { if (it.isFocused) onFocused() }
                .clip(shape)
                .background(bgColor)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (dubbing.isNotBlank()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = dubbing,
                            style = MaterialTheme.typography.labelSmall,
                            color = textColor.copy(alpha = 0.62f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (views != null && views > 0) {
                            Icon(
                                imageVector = Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = textColor.copy(alpha = 0.62f),
                                modifier = Modifier.size(13.dp),
                            )
                            Text(
                                text = views.formatCompactCount(),
                                style = MaterialTheme.typography.labelSmall,
                                color = textColor.copy(alpha = 0.62f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
            Text(
                text = stringResource(R.string.details_balancer_open),
                style = MaterialTheme.typography.labelSmall,
                color = textColor.copy(alpha = 0.64f),
                maxLines = 1,
            )
        }
    } else {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(Color.White.copy(alpha = 0.05f))
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BalancerOptionTexts(
                label = label,
                meta = stringResource(R.string.details_balancer_unsupported_hint),
                color = Color.White.copy(alpha = 0.36f),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.details_unsupported),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.30f),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun BalancerOptionTexts(
    label: String,
    meta: String?,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = color.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
