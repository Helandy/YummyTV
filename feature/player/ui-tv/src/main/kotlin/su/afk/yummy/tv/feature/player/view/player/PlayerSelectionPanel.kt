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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.R

@Composable
internal fun PlayerSelectionPanel(
    visible: Boolean,
    title: String,
    items: List<String>,
    selectedIndex: Int,
    selectedFocusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    itemMeta: @Composable (index: Int) -> String? = { null },
    itemMetaContent: @Composable (index: Int, contentColor: Color) -> Unit = { index, contentColor ->
        val meta = itemMeta(index)
        if (!meta.isNullOrBlank()) {
            Text(
                text = meta,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.62f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    },
    onItemSelected: (index: Int) -> Unit,
) {
    AnimatedVisibility(
        visible = visible && items.isNotEmpty(),
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        val maxPanelHeight = (LocalConfiguration.current.screenHeightDp * 0.7f).dp
        Column(
            modifier = Modifier
                .width(336.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xE6121214))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
                .heightIn(max = maxPanelHeight)
                .verticalScroll(rememberScrollState())
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.62f),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
            items.forEachIndexed { idx, label ->
                val selected = idx == selectedIndex
                PlayerSelectionItem(
                    label = label,
                    metaContent = { contentColor -> itemMetaContent(idx, contentColor) },
                    selected = selected,
                    modifier = if (selected) Modifier.focusRequester(selectedFocusRequester) else Modifier,
                    onClick = { onItemSelected(idx) },
                )
            }
        }
    }
}

@Composable
private fun PlayerSelectionItem(
    label: String,
    metaContent: @Composable (contentColor: Color) -> Unit,
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
        label = "selection_item_bg",
    )
    val borderColor by animateColorAsState(
        targetValue = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "selection_item_border",
    )
    val contentColor = if (focused || selected) Color.Black else Color.White
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .clip(shape)
                .background(background)
                .border(2.dp, borderColor, shape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
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
                metaContent(contentColor)
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
}
