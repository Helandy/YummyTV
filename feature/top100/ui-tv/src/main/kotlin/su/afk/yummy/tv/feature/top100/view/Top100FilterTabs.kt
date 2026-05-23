package su.afk.yummy.tv.feature.top100.view

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalTopBarFocusRequester
import su.afk.yummy.tv.domain.top100.AnimeTopType
import su.afk.yummy.tv.feature.top100.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Top100FilterSidePanel(
    selectedType: AnimeTopType,
    onTypeSelected: (AnimeTopType) -> Unit,
    contentFocusRequester: FocusRequester,
    typeFocusRequesters: List<FocusRequester>,
    onFocusChange: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedIndex = AnimeTopType.entries.indexOf(selectedType).coerceAtLeast(0)
    var focusedIndex by remember { mutableIntStateOf(selectedIndex) }
    val panelWidth by animateDpAsState(
        targetValue = if (expanded) 148.dp else 52.dp,
        animationSpec = tween(durationMillis = 200),
        label = "side_panel_width",
    )

    LaunchedEffect(selectedIndex) {
        focusedIndex = selectedIndex
    }

    Column(
        modifier = modifier
            .width(panelWidth)
            .fillMaxHeight()
            .onFocusChanged {
                expanded = it.hasFocus
                onFocusChange(it.hasFocus)
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionUp -> {
                        if (focusedIndex > 0) {
                            focusedIndex -= 1
                            typeFocusRequesters[focusedIndex].requestFocus()
                            true
                        } else {
                            false
                        }
                    }
                    Key.DirectionDown -> {
                        if (focusedIndex < typeFocusRequesters.lastIndex) {
                            focusedIndex += 1
                            typeFocusRequesters[focusedIndex].requestFocus()
                        }
                        true
                    }
                    else -> false
                }
            }
            .focusProperties {
                onEnter = {
                    typeFocusRequesters.getOrNull(selectedIndex)?.requestFocus()
                }
            }
            .focusGroup()
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp),
            )
            .padding(vertical = 24.dp, horizontal = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimeTopType.entries.forEachIndexed { index, type ->
            SidePanelItem(
                label = type.label(),
                shortLabel = type.shortLabel(),
                selected = selectedType == type,
                expanded = expanded,
                onSelected = { onTypeSelected(type) },
                contentFocusRequester = contentFocusRequester,
                focusRequester = typeFocusRequesters[index],
                upFocusRequester = typeFocusRequesters.getOrNull(index - 1),
                downFocusRequester = typeFocusRequesters.getOrNull(index + 1) ?: FocusRequester.Cancel,
                onFocused = { focusedIndex = index },
            )
        }
    }
}

@Composable
private fun SidePanelItem(
    label: String,
    shortLabel: String,
    selected: Boolean,
    expanded: Boolean,
    onSelected: () -> Unit,
    contentFocusRequester: FocusRequester,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
) {
    val topBarFocusRequester = LocalTopBarFocusRequester.current
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = Modifier
            .width(if (expanded) 136.dp else 40.dp)
            .focusRequester(focusRequester)
            .focusProperties {
                right = contentFocusRequester
                (upFocusRequester ?: topBarFocusRequester)?.let { up = it }
                downFocusRequester?.let { down = it }
            }
            .onFocusChanged { if (it.isFocused || it.hasFocus) onFocused() }
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
                shape = shape,
            )
            .tvFocusableClick(onClick = onSelected, shape = shape)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (expanded) label else shortLabel,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Clip,
        )
    }
}

@Composable
private fun AnimeTopType.label(): String = when (this) {
    AnimeTopType.TV -> stringResource(R.string.top100_type_tv)
    AnimeTopType.MOVIE -> stringResource(R.string.top100_type_movie)
    AnimeTopType.ONA -> name
}

@Composable
private fun AnimeTopType.shortLabel(): String = when (this) {
    AnimeTopType.TV -> stringResource(R.string.top100_type_tv_short)
    AnimeTopType.MOVIE -> stringResource(R.string.top100_type_movie_short)
    AnimeTopType.ONA -> name
}
