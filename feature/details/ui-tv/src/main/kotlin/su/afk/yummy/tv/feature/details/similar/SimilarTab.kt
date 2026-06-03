package su.afk.yummy.tv.feature.details.similar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerContainer
import su.afk.yummy.tv.core.designsystem.presenter.focus.focusRestorerItem
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberFocusRestorerState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.SimilarUiState
import su.afk.yummy.tv.feature.details.view.common.RelatedTitleCard

private val RelatedCardWidth = 188.dp

@Composable
internal fun SimilarTab(
    state: SimilarUiState,
    fromAi: Boolean,
    onToggle: () -> Unit,
    onAnimeSelected: (Int) -> Unit,
    onItemFocused: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SourceToggle(
            fromAi = fromAi,
            onToggle = onToggle,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        when (state) {
            SimilarUiState.Loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }

            SimilarUiState.Empty -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.details_similar_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp),
                )
            }

            is SimilarUiState.Content -> {
                val restorerState = rememberFocusRestorerState()
                val focusManager = LocalFocusManager.current
                val directionKeyModifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> { focusManager.moveFocus(FocusDirection.Up); true }
                        Key.DirectionDown -> { focusManager.moveFocus(FocusDirection.Down); true }
                        else -> false
                    }
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val sideInset = ((maxWidth - RelatedCardWidth) / 2).coerceAtLeast(24.dp)
                    LazyRow(
                        state = rememberLazyListState(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = PaddingValues(horizontal = sideInset, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRestorerContainer(restorerState),
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> item.animeId },
                        ) { index, item ->
                            val posterUrl = item.poster?.run { big ?: medium ?: fullsize ?: small }
                            val meta = listOfNotNull(item.year?.toString(), item.type).joinToString(" · ")
                            RelatedTitleCard(
                                title = item.title,
                                posterUrl = posterUrl,
                                onClick = { onAnimeSelected(item.animeId) },
                                rating = item.rating,
                                meta = meta,
                                onFocused = { onItemFocused(item.animeId) },
                                modifier = Modifier
                                    .focusRestorerItem(index, restorerState)
                                    .then(directionKeyModifier),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceToggle(fromAi: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleChip(
            label = stringResource(R.string.details_similar_users),
            selected = !fromAi,
            onClick = { if (fromAi) onToggle() },
        )
        ToggleChip(
            label = stringResource(R.string.details_similar_ai),
            selected = fromAi,
            onClick = { if (!fromAi) onToggle() },
        )
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = Modifier.tvFocusableClick(onClick = onClick, shape = shape),
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary
                else Color.Transparent,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
