package su.afk.yummy.tv.feature.top.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.domain.top.model.AnimeTopType
import su.afk.yummy.tv.feature.top.utils.label

@Composable
internal fun TopFilterTabs(
    selectedType: AnimeTopType,
    contentCanFocus: Boolean,
    onTypeSelected: (AnimeTopType) -> Unit,
    contentFocusRequester: FocusRequester,
    typeFocusRequesters: List<FocusRequester>,
    mainMenuFocusRequester: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = AnimeTopType.entries.indexOf(selectedType).coerceAtLeast(0)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = TvScreenPadding.Horizontal,
                top = TvScreenPadding.Vertical,
                end = TvScreenPadding.Horizontal,
            )
            .focusProperties {
                onEnter = {
                    typeFocusRequesters.getOrNull(selectedIndex)?.requestFocus()
                }
            }
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimeTopType.entries.forEachIndexed { index, type ->
            TopFilterTabItem(
                label = type.label(),
                selected = selectedType == type,
                onActivated = {
                    onTypeSelected(type)
                    runCatching { contentFocusRequester.requestFocus() }
                },
                contentFocusRequester = contentFocusRequester,
                focusRequester = typeFocusRequesters[index],
                contentCanFocus = contentCanFocus,
                leftFocusRequester = typeFocusRequesters.getOrNull(index - 1)
                    ?: mainMenuFocusRequester.takeIf { index == 0 },
                rightFocusRequester = typeFocusRequesters.getOrNull(index + 1)
                    ?: contentFocusRequester.takeIf { contentCanFocus },
                onFocused = {
                    if (selectedType != type) onTypeSelected(type)
                },
            )
        }
    }
}

@Composable
private fun TopFilterTabItem(
    label: String,
    selected: Boolean,
    onActivated: () -> Unit,
    contentFocusRequester: FocusRequester,
    focusRequester: FocusRequester,
    contentCanFocus: Boolean,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
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
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                if (contentCanFocus) {
                    down = contentFocusRequester
                }
                leftFocusRequester?.let { left = it }
                rightFocusRequester?.let { right = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        leftFocusRequester?.let {
                            runCatching { it.requestFocus() }
                            true
                        } ?: false
                    }

                    Key.DirectionRight -> {
                        rightFocusRequester?.let {
                            runCatching { it.requestFocus() }
                        }
                        true
                    }

                    Key.DirectionDown, Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        if (contentCanFocus) {
                            onActivated()
                        } else {
                            onFocused()
                        }
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged { if (it.isFocused || it.hasFocus) onFocused() }
            .background(
                color = if (focused) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                else Color.Transparent,
                shape = shape,
            )
            .tvFocusableClick(
                onClick = onActivated,
                shape = shape,
                interactionSource = interactionSource,
                focusedScale = 1f,
                focusedBorderColor = Color.Transparent,
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
