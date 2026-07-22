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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
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
private val SimilarPosterHeight = 214.dp

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
    // Вертикальная навигация связывается явно: кнопки голосования перекрывают карточку по
    // границам, из-за чего focus search вверх уходил на них и фокус запирался внизу экрана.
    val visibilityButtonFocusRequester = remember { FocusRequester() }
    val sourceToggleFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier.padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TvSimilarRecommendationVisibilityButton(
            ignored = ignored,
            enabled = !recommendationMutationPending,
            onClick = onRecommendationVisibilityToggled,
            modifier = Modifier
                .focusRequester(visibilityButtonFocusRequester)
                .focusProperties { down = sourceToggleFocusRequester },
        )

        SourceToggle(
            fromAi = fromAi,
            onToggle = onToggle,
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .focusProperties { up = visibilityButtonFocusRequester },
            focusRequester = sourceToggleFocusRequester,
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
                val listState = rememberLazyListState()
                val itemIds = remember(state.items) { state.items.map { it.animeId } }
                val focusRequesters = remember(itemIds) {
                    List(state.items.size) { FocusRequester() }
                }
                val voteFocusRequesters = remember(itemIds) {
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
                            RelatedTitleCard(
                                title = item.title,
                                posterUrl = posterUrl,
                                onClick = { onAnimeSelected(item.animeId) },
                                rating = item.rating,
                                year = item.year,
                                meta = meta,
                                onFocused = { rememberFocusedItem(index) },
                                // Постер ниже дефолтного: карточке нужно место под голосование,
                                // иначе ряд не влезает в экран и футер обрезается.
                                posterHeight = SimilarPosterHeight,
                                modifier = Modifier
                                    .focusRequester(focusRequesters[index])
                                    .focusProperties {
                                        up = sourceToggleFocusRequester
                                        if (!fromAi) down = voteFocusRequesters[index]
                                    },
                                footer = {
                                    if (!fromAi) {
                                        TvSimilarVoteButtons(
                                            item = item,
                                            enabled = item.animeId !in pendingVoteAnimeIds,
                                            onVote = { vote -> onVote(item.animeId, vote) },
                                            modifier = Modifier
                                                .focusProperties {
                                                    up = focusRequesters[index]
                                                    // Ниже голосования ничего нет — не даём
                                                    // фокусу перескочить в начало экрана.
                                                    down = FocusRequester.Cancel
                                                },
                                            focusRequester = voteFocusRequesters[index],
                                        )
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceToggle(
    fromAi: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    /** Точка входа фокуса — активный чип, чтобы фокус не приходил на неактивный источник. */
    focusRequester: FocusRequester? = null,
) {
    val shape = RoundedCornerShape(24.dp)
    val selectedModifier = focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier
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
            modifier = if (fromAi) Modifier else selectedModifier,
        )
        ToggleChip(
            label = stringResource(R.string.details_similar_ai),
            selected = fromAi,
            onClick = { if (!fromAi) onToggle() },
            modifier = if (fromAi) selectedModifier else Modifier,
        )
    }
}

@Composable
private fun ToggleChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(20.dp)
    Surface(
        modifier = modifier.tvFocusableClick(onClick = onClick, shape = shape),
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
