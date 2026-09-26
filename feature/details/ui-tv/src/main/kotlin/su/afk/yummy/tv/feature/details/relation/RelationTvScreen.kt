package su.afk.yummy.tv.feature.details.relation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.focus.rememberTvGridStartExtent
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.designsystem.tv.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.tv.TvTitleCard
import su.afk.yummy.tv.core.utils.lazylist.lazyKey
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationItem
import su.afk.yummy.tv.domain.anime.model.AnimeRelationSubGenre
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.relation.model.RelationType
import su.afk.yummy.tv.feature.details.relation.view.RelationTvHeaderCard
import su.afk.yummy.tv.feature.details.view.common.DetailsError

@Preview(
    name = "Director",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
@Composable
private fun RelationTvDirectorScreenPreview() = ScreenPreviewTheme {
    RelationTvScreen(
        state = RelationState.State(
            relationType = RelationType.DIRECTOR,
            isLoading = false,
            relation = previewDirectorRelation(),
        ),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@Preview(
    name = "Empty genre",
    device = "spec:width=1920dp,height=1080dp,dpi=160",
    uiMode = android.content.res.Configuration.UI_MODE_TYPE_TELEVISION,
    showBackground = true,
)
@Composable
private fun RelationTvEmptyGenreScreenPreview() = ScreenPreviewTheme {
    RelationTvScreen(
        state = RelationState.State(
            relationType = RelationType.GENRE,
            isLoading = false,
            relation = previewEmptyGenreRelation(),
        ),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RelationTvScreen(
    state: RelationState.State,
    effect: Flow<RelationState.Effect>,
    onEvent: (RelationState.Event) -> Unit,
) {
    BackHandler { onEvent(RelationState.Event.BackSelected) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        when {
            state.isLoading && state.relation == null -> TvLoadingScreen()
            state.relation == null -> DetailsError(
                message = state.error.orEmpty(),
                onRetry = { onEvent(RelationState.Event.RetrySelected) },
            )

            else -> {
                val relation = checkNotNull(state.relation)
                val gridState = rememberLazyGridState()
                val headerFocusRequester = remember { FocusRequester() }

                // Запоминаем чип поджанра/карточку связанного аниме, с которых ушли,
                // чтобы при возврате назад вернуть фокус именно на них. Единый sealed-тип
                // гарантирует, что "последней целью" может быть только что-то одно —
                // в отличие от пары независимых nullable-полей, где легко забыть сбросить
                // одно при установке другого.
                var lastFocusTarget by rememberSaveable(
                    relation.title,
                    stateSaver = RelationFocusTargetSaver,
                ) {
                    mutableStateOf<RelationFocusTarget?>(null)
                }
                val subGenreFocusRequesters = remember(relation.subGenres) {
                    relation.subGenres.associate { it.id to FocusRequester() }
                }
                val animeFocusRequesters = remember(relation.anime) {
                    relation.anime.associate { it.animeId to FocusRequester() }
                }
                val animeFocusRequesterList = remember(relation.anime, animeFocusRequesters) {
                    relation.anime.map { animeFocusRequesters.getValue(it.animeId) }
                }
                val scope = rememberCoroutineScope()
                val animeIndexById = remember(relation.anime) {
                    relation.anime.mapIndexed { index, item -> item.animeId to index }.toMap()
                }

                LaunchedEffect(relation.title) {
                    when (val target = lastFocusTarget) {
                        is RelationFocusTarget.SubGenre -> {
                            val requester = subGenreFocusRequesters[target.id]
                            if (requester != null) {
                                restoreRelationItemFocus(
                                    gridState,
                                    itemIndex = 0,
                                    requester = requester
                                )
                            } else {
                                headerFocusRequester.requestFocus()
                            }
                        }

                        is RelationFocusTarget.Anime -> {
                            val index = animeIndexById[target.id]
                            val requester = animeFocusRequesters[target.id]
                            if (index != null && requester != null) {
                                restoreRelationItemFocus(
                                    gridState,
                                    itemIndex = index + 1,
                                    requester = requester,
                                )
                            } else {
                                headerFocusRequester.requestFocus()
                            }
                        }

                        null -> headerFocusRequester.requestFocus()
                    }
                }
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val cardWidth = currentTvTitleCardDimensions().width
                    val horizontalSpacing = TvCardSpacing.Horizontal
                    val gridColumnCount =
                        (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                            (cardWidth.value + horizontalSpacing.value)).toInt()
                            .coerceAtLeast(1)
                    val gridStartExtent =
                        rememberTvGridStartExtent(TvScreenPadding.Vertical, TvCardSpacing.Vertical)

                    CompositionLocalProvider(
                        LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(TvCardSpacing.Vertical),
                    ) {
                        LazyVerticalGrid(
                            state = gridState,
                            columns = GridCells.Adaptive(cardWidth),
                            contentPadding = PaddingValues(
                                horizontal = TvScreenPadding.Horizontal,
                                vertical = TvScreenPadding.Vertical,
                            ),
                            horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                            verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                        ) {
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Column(
                                    modifier = gridStartExtent.measure,
                                    verticalArrangement = Arrangement.spacedBy(20.dp),
                                ) {
                                    RelationTvHeaderCard(
                                        relationType = state.relationType,
                                        relation = relation,
                                        onSubGenreSelected = { genreId ->
                                            lastFocusTarget = RelationFocusTarget.SubGenre(genreId)
                                            onEvent(RelationState.Event.SubGenreSelected(genreId))
                                        },
                                        subGenreFocusRequesters = subGenreFocusRequesters,
                                        modifier = Modifier.focusRequester(headerFocusRequester),
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.details_related_anime_count,
                                            relation.anime.size,
                                        ),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                    )
                                    if (relation.anime.isEmpty()) {
                                        Text(
                                            text = stringResource(R.string.details_related_empty),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                            itemsIndexed(relation.anime, key = { _, item -> lazyKey("relation", item.animeId) }) { index, item ->
                                TvTitleCard(
                                    title = item.title,
                                    posterUrl = item.posterUrl,
                                    onClick = {
                                        lastFocusTarget = RelationFocusTarget.Anime(item.animeId)
                                        onEvent(RelationState.Event.AnimeSelected(item.animeId))
                                    },
                                    modifier = Modifier
                                        .focusRequester(animeFocusRequesterList[index])
                                        .tvWholeItemBringIntoView(gridStartExtent.takeIf { index < gridColumnCount })
                                        .tvLazyGridRowFocusNavigation(
                                            index = index,
                                            columnCount = gridColumnCount,
                                            itemCount = animeFocusRequesterList.size,
                                            gridState = gridState,
                                            scope = scope,
                                            focusRequesterAt = animeFocusRequesterList::getOrNull,
                                            // нулевой lazy-индекс занимает шапка связи
                                            lazyIndexOffset = 1,
                                        ),
                                    posterOverlay = {
                                        item.rating?.let { rating ->
                                            RatingBadge(
                                                rating = rating,
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(4.dp),
                                            )
                                        }
                                        item.year?.let { year ->
                                            Text(
                                                text = year.toString(),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(4.dp)
                                                    .background(
                                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                                        RoundedCornerShape(4.dp),
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 3.dp),
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

private sealed interface RelationFocusTarget {
    data class SubGenre(val id: Int) : RelationFocusTarget
    data class Anime(val id: Int) : RelationFocusTarget
}

private val RelationFocusTargetSaver = listSaver<RelationFocusTarget?, Any>(
    save = { target ->
        when (target) {
            null -> emptyList()
            is RelationFocusTarget.SubGenre -> listOf(true, target.id)
            is RelationFocusTarget.Anime -> listOf(false, target.id)
        }
    },
    restore = { saved ->
        if (saved.isEmpty()) {
            null
        } else {
            val id = saved[1] as Int
            if (saved[0] as Boolean) RelationFocusTarget.SubGenre(id) else RelationFocusTarget.Anime(
                id
            )
        }
    },
)

private suspend fun restoreRelationItemFocus(
    gridState: LazyGridState,
    itemIndex: Int,
    requester: FocusRequester,
) {
    gridState.scrollToItem(itemIndex)
    requestFocusUntilTimeout(requester)
}

private fun previewDirectorRelation() = AnimeRelation(
    title = "Hayao Miyazaki",
    secondaryTitle = "宮崎 駿",
    anime = listOf(
        AnimeRelationItem(
            animeId = 1,
            title = "Spirited Away",
            posterUrl = null,
            rating = 8.9,
            year = 2001,
        ),
        AnimeRelationItem(
            animeId = 2,
            title = "Howl's Moving Castle",
            posterUrl = null,
            rating = 8.7,
            year = 2004,
        ),
    ),
)

private fun previewEmptyGenreRelation() = AnimeRelation(
    title = "Приключения",
    description = "Истории о путешествиях, открытиях и необычных мирах.",
    subGenres = listOf(
        AnimeRelationSubGenre(id = 1, title = "Фэнтези"),
        AnimeRelationSubGenre(id = 2, title = "Экшен"),
    ),
    anime = emptyList(),
)
