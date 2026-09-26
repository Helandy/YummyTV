package su.afk.yummy.tv.feature.details.similar.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
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
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.focus.TvCenteredCarouselBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.model.SimilarUiState
import su.afk.yummy.tv.feature.details.view.common.RelatedTitleCard

private val RelatedCardWidth = 188.dp

@OptIn(ExperimentalFoundationApi::class)
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
    val sourceToggleFocusRequester = remember { FocusRequester() }

    Column(
        modifier = modifier.padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Кнопка и переключатель в одном ряду — освобождённая высота уходит на постеры
        // тех же пропорций, что в «Порядке просмотра».
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TvSimilarRecommendationVisibilityButton(
                ignored = ignored,
                enabled = !recommendationMutationPending,
                onClick = onRecommendationVisibilityToggled,
            )

            SourceToggle(
                fromAi = fromAi,
                onToggle = onToggle,
                focusRequester = sourceToggleFocusRequester,
            )
        }

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
                // Пока карточка ни разу не фокусировалась, дефолтный фокус — на кнопке видимости
                // рекомендации вверху вкладки; как только фокус побывал на карточке, эта карточка
                // регистрируется как preferred-фокус экрана, иначе после Back из деталей другого
                // тайтла фокус улетает на первый фокусируемый элемент вкладки (кнопку), т.к. сам
                // LazyRow не является точкой входа фокуса и его focusRestorer не запрашивается явно.
                var hasFocusedItem by rememberSaveable(fromAi) { mutableStateOf(false) }
                val registerPreferredContentFocusRequester =
                    LocalPreferredContentFocusRequester.current

                fun restoreIndex(): Int {
                    if (state.items.isEmpty()) return 0
                    val keyedIndex = lastFocusedItemId?.let { id ->
                        state.items.indexOfFirst { it.animeId == id }
                    } ?: -1
                    return keyedIndex.takeIf { it >= 0 }
                        ?: lastFocusedIndex.coerceIn(0, state.items.lastIndex)
                }

                fun rememberFocusedItem(index: Int) {
                    hasFocusedItem = true
                    lastFocusedIndex = index
                    lastFocusedItemId = state.items.getOrNull(index)?.animeId
                }

                val preferredContentFocusRequester =
                    if (hasFocusedItem) focusRequesters.getOrNull(restoreIndex()) else null

                DisposableEffect(
                    preferredContentFocusRequester,
                    registerPreferredContentFocusRequester,
                ) {
                    registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
                    onDispose { registerPreferredContentFocusRequester?.invoke(null) }
                }

                BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                    val sideInset = ((maxWidth - RelatedCardWidth) / 2).coerceAtLeast(24.dp)
                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides TvCenteredCarouselBringIntoViewSpec,
                    ) {
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
                                key = { index, item -> lazyKey("similar", item.animeId, index) },
                            ) { index, item ->
                                val posterUrl =
                                    item.poster?.run { big ?: medium ?: fullsize ?: small }
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
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            Color.Transparent
        },
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}
