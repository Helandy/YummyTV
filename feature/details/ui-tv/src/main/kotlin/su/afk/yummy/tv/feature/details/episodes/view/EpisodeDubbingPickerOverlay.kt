package su.afk.yummy.tv.feature.details.episodes.view

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VideoLibrary
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
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import java.util.Locale

@Composable
internal fun EpisodeDubbingPickerOverlay(
    selection: EpisodesState.EpisodeDubbingSelection,
    onSelected: (EpisodesState.EpisodeDubbingOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val firstFocusRequester = remember { FocusRequester() }
    var focusedOptionIndex by remember(selection.options) { mutableIntStateOf(0) }

    LaunchedEffect(selection.options) {
        if (selection.options.isNotEmpty()) {
            focusManager.clearFocus(force = true)
            withFrameNanos { }
            runCatching { firstFocusRequester.requestFocus() }
            withFrameNanos { }
            runCatching { firstFocusRequester.requestFocus() }
        }
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
            Column(
                modifier = Modifier
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

                            Key.DirectionUp -> focusedOptionIndex == 0
                            Key.DirectionDown -> focusedOptionIndex == selection.options.lastIndex
                            else -> false
                        }
                    }
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.details_episode_dubbings_title,
                        selection.episode
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.70f),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                )
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 430.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(
                        items = selection.options,
                        key = { _, option -> option.item.name },
                    ) { index, option ->
                        EpisodeDubbingOptionItem(
                            option = option,
                            modifier = if (index == 0) {
                                Modifier.focusRequester(firstFocusRequester)
                            } else {
                                Modifier
                            },
                            onFocused = { focusedOptionIndex = index },
                            onClick = { onSelected(option) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeDubbingOptionItem(
    option: EpisodesState.EpisodeDubbingOption,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(12.dp)
    val contentColor = if (focused) Color.Black else Color.White
    Column(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clip(shape)
            .background(if (focused) Color.White else Color.White.copy(alpha = 0.10f))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(
            text = option.item.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Visibility,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.62f),
                modifier = Modifier.size(13.dp),
            )
            Text(
                text = option.item.views.formatCompactCount(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.62f),
            )
            Icon(
                imageVector = Icons.Filled.VideoLibrary,
                contentDescription = null,
                tint = contentColor.copy(alpha = 0.62f),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(13.dp),
            )
            Text(
                text = option.item.episodeCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.62f),
            )
        }
        if (option.item.supportedBalancers.isNotBlank()) {
            Text(
                text = option.item.supportedBalancers,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Int.formatCompactCount(): String = when {
    this >= 1_000_000 -> stringResource(
        R.string.details_count_millions,
        (this / 1_000_000f).formatCompactDecimal(),
    )

    this >= 1_000 -> stringResource(
        R.string.details_count_thousands,
        (this / 1_000f).formatCompactDecimal(),
    )

    else -> toString()
}

private fun Float.formatCompactDecimal(): String =
    if (this % 1f == 0f) toInt().toString() else String.format(Locale.US, "%.1f", this)
