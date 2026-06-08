package su.afk.yummy.tv.feature.top.view

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
                rightFocusRequester = typeFocusRequesters.getOrNull(index + 1),
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

    Box(
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
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                shape = shape,
            )
            .tvFocusableClick(onClick = onActivated, shape = shape)
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
