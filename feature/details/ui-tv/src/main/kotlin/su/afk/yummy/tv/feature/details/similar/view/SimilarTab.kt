package su.afk.yummy.tv.feature.details.similar.view

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Recommend
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
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
    ignored: Boolean,
    recommendationMutationPending: Boolean,
    pendingVoteAnimeIds: Set<Int>,
    onRecommendationVisibilityToggled: () -> Unit,
    onVote: (Int, AnimeRecommendationVote) -> Unit,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.padding(vertical = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TvSimilarRecommendationVisibilityButton(
            ignored = ignored,
            enabled = !recommendationMutationPending,
            onClick = onRecommendationVisibilityToggled,
        )

        SourceToggle(
            fromAi = fromAi,
            onToggle = onToggle,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        when (state) {
            SimilarUiState.Loading -> SimilarLoadingState(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            )

            SimilarUiState.Empty -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TvStateMessage(
                    title = stringResource(R.string.details_similar_empty),
                    icon = Icons.Outlined.Recommend,
                )
            }

            is SimilarUiState.Error -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TvStateMessage(
                    title = state.message ?: stringResource(R.string.details_similar_empty),
                    icon = Icons.Filled.Warning,
                    onRetry = onRetry,
                )
            }

            is SimilarUiState.Content -> {
                val focusManager = LocalFocusManager.current
                val listState = rememberLazyListState()
                val itemIds = remember(state.items) { state.items.map { it.animeId } }
                val focusRequesters = remember(itemIds) {
                    List(state.items.size) { FocusRequester() }
                }
                var lastFocusedItemId by rememberSaveable(fromAi) { mutableStateOf<Int?>(null) }
                var lastFocusedIndex by rememberSaveable(fromAi) { mutableIntStateOf(0) }

                fun restoreIndex(): Int {
                    if (state.items.isEmpty()) return 0
                    val keyedIndex = lastFocusedItemId?.let { id ->
                        state.items.indexOfFirst { it.animeId == id }
                    } ?: -1
                    return keyedIndex.takeIf { it >= 0 }
                        ?: lastFocusedIndex.coerceIn(0, state.items.lastIndex)
                }

                fun rememberFocusedItem(index: Int) {
                    lastFocusedIndex = index
                    lastFocusedItemId = state.items.getOrNull(index)?.animeId
                }

                val directionKeyModifier = Modifier.onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionUp -> {
                            focusManager.moveFocus(FocusDirection.Up); true
                        }

                        Key.DirectionDown -> {
                            focusManager.moveFocus(FocusDirection.Down); true
                        }

                        else -> false
                    }
                }
                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val sideInset = ((maxWidth - RelatedCardWidth) / 2).coerceAtLeast(24.dp)
                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                        contentPadding = PaddingValues(horizontal = sideInset, vertical = 8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .tvFocusRestorer(
                                fallback = focusRequesters.getOrNull(restoreIndex())
                                    ?: FocusRequester.Default,
                            ),
                    ) {
                        itemsIndexed(
                            items = state.items,
                            key = { _, item -> item.animeId },
                        ) { index, item ->
                            val posterUrl = item.poster?.run { big ?: medium ?: fullsize ?: small }
                            val meta =
                                listOfNotNull(item.type).joinToString(" · ")
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                RelatedTitleCard(
                                    title = item.title,
                                    posterUrl = posterUrl,
                                    onClick = { onAnimeSelected(item.animeId) },
                                    rating = item.rating,
                                    year = item.year,
                                    meta = meta,
                                    onFocused = { rememberFocusedItem(index) },
                                    modifier = Modifier
                                        .focusRequester(focusRequesters[index])
                                        .then(directionKeyModifier),
                                )
                                if (!fromAi) {
                                    TvSimilarVoteButtons(
                                        item = item,
                                        enabled = item.animeId !in pendingVoteAnimeIds,
                                        onVote = { vote -> onVote(item.animeId, vote) },
                                    )
                                }
                            }
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
