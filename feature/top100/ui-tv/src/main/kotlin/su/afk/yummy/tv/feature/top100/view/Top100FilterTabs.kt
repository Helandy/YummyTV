package su.afk.yummy.tv.feature.top100.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import su.afk.yummy.tv.domain.top100.model.AnimeTopType
import su.afk.yummy.tv.feature.top100.utils.label

@Composable
internal fun Top100FilterTabs(
    selectedType: AnimeTopType,
    onTypeSelected: (AnimeTopType) -> Unit,
    contentFocusRequester: FocusRequester,
    typeFocusRequesters: List<FocusRequester>,
    onFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val selectedIndex = AnimeTopType.entries.indexOf(selectedType).coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }

    LaunchedEffect(selectedIndex) {
        focusedIndex = selectedIndex
    }

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
            .onFocusChanged { onFocusChange(it.hasFocus) }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        if (focusedIndex > 0) {
                            focusedIndex -= 1
                            typeFocusRequesters[focusedIndex].requestFocus()
                            true
                        } else {
                            false
                        }
                    }

                    Key.DirectionRight -> {
                        if (focusedIndex < typeFocusRequesters.lastIndex) {
                            focusedIndex += 1
                            typeFocusRequesters[focusedIndex].requestFocus()
                        }
                        true
                    }

                    else -> false
                }
            }
            .focusGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimeTopType.entries.forEachIndexed { index, type ->
            Top100FilterTabItem(
                label = type.label(),
                selected = selectedType == type,
                onSelected = { onTypeSelected(type) },
                contentFocusRequester = contentFocusRequester,
                focusRequester = typeFocusRequesters[index],
                leftFocusRequester = typeFocusRequesters.getOrNull(index - 1),
                rightFocusRequester = typeFocusRequesters.getOrNull(index + 1),
                onFocused = { focusedIndex = index },
            )
        }
    }
}

@Composable
private fun Top100FilterTabItem(
    label: String,
    selected: Boolean,
    onSelected: () -> Unit,
    contentFocusRequester: FocusRequester,
    focusRequester: FocusRequester,
    leftFocusRequester: FocusRequester?,
    rightFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                down = contentFocusRequester
                leftFocusRequester?.let { left = it }
                rightFocusRequester?.let { right = it }
            }
            .onFocusChanged { if (it.isFocused || it.hasFocus) onFocused() }
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                shape = shape,
            )
            .tvFocusableClick(onClick = onSelected, shape = shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
