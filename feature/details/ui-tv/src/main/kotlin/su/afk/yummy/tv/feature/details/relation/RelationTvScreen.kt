package su.afk.yummy.tv.feature.details.relation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import su.afk.yummy.tv.core.designsystem.presenter.components.RatingBadge
import su.afk.yummy.tv.core.designsystem.presenter.components.TvTitleCard
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
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
                val firstCardFocusRequester = remember { FocusRequester() }
                LaunchedEffect(relation.title, relation.anime.isNotEmpty()) {
                    if (relation.anime.isNotEmpty()) firstCardFocusRequester.requestFocus()
                }
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(currentTvTitleCardDimensions().width),
                    contentPadding = PaddingValues(
                        horizontal = TvScreenPadding.Horizontal,
                        vertical = TvScreenPadding.Vertical,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                    verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                            RelationTvHeaderCard(
                                relationType = state.relationType,
                                relation = relation,
                                onSubGenreSelected = { genreId ->
                                    onEvent(RelationState.Event.SubGenreSelected(genreId))
                                },
                            )
                            Text(
                                text = stringResource(R.string.details_related_anime),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            if (relation.anime.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.details_related_empty),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    items(relation.anime, key = { it.animeId }) { item ->
                        TvTitleCard(
                            title = item.title,
                            posterUrl = item.posterUrl,
                            onClick = {
                                onEvent(RelationState.Event.AnimeSelected(item.animeId))
                            },
                            modifier = if (item == relation.anime.firstOrNull()) {
                                Modifier.focusRequester(firstCardFocusRequester)
                            } else Modifier,
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
