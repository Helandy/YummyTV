package su.afk.yummy.tv.feature.details.mobile.relation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileStateContent
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.designsystem.presenter.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationItem
import su.afk.yummy.tv.domain.anime.model.AnimeRelationSubGenre
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.mobile.relation.utils.labelRes
import su.afk.yummy.tv.feature.details.mobile.relation.view.RelationMobileHeaderCard
import su.afk.yummy.tv.feature.details.relation.RelationState
import su.afk.yummy.tv.feature.details.relation.model.RelationType

@Preview(name = "Studio", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun RelationMobileStudioScreenPreview() = ScreenPreviewTheme {
    RelationMobileScreen(
        state = RelationState.State(
            relationType = RelationType.STUDIO,
            isLoading = false,
            relation = previewStudioRelation(),
        ),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@Preview(
    name = "Empty genre",
    device = "spec:width=412dp,height=915dp,dpi=420",
    showBackground = true
)
@Composable
private fun RelationMobileEmptyGenreScreenPreview() = ScreenPreviewTheme {
    RelationMobileScreen(
        state = RelationState.State(
            relationType = RelationType.GENRE,
            isLoading = false,
            relation = previewEmptyGenreRelation(),
        ),
        effect = emptyFlow(),
        onEvent = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelationMobileScreen(
    state: RelationState.State,
    effect: Flow<RelationState.Effect>,
    onEvent: (RelationState.Event) -> Unit,
) {
    BaseScreen(
        isScroll = false,
        topBar = {
            MobileTopBar(
                title = stringResource(state.relationType.labelRes()),
                onBack = { onEvent(RelationState.Event.BackSelected) },
            )
        },
    ) {
        MobileStateContent(
            isLoading = state.isLoading,
            error = state.error,
            onRetry = { onEvent(RelationState.Event.RetrySelected) },
            empty = state.relation == null,
        ) {
            val relation = state.relation ?: return@MobileStateContent
            MobilePosterGrid(contentPadding = PaddingValues(bottom = 16.dp)) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        RelationMobileHeaderCard(
                            relationType = state.relationType,
                            relation = relation,
                            onSubGenreSelected = { genreId ->
                                onEvent(RelationState.Event.SubGenreSelected(genreId))
                            },
                        )
                        Text(
                            text = stringResource(R.string.details_mobile_related_anime),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 20.dp),
                        )
                        if (relation.anime.isEmpty()) {
                            Text(
                                text = stringResource(R.string.details_mobile_related_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }
                relation.anime.forEach { item ->
                    item(key = item.animeId) {
                        MobilePosterCard(
                            title = item.title,
                            posterUrl = item.posterUrl,
                            rating = item.rating,
                            posterOverlay = {
                                item.year?.let { year ->
                                    Text(
                                        text = year.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.inverseSurface,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                            .background(
                                                MaterialTheme.colorScheme.inverseOnSurface,
                                                RoundedCornerShape(4.dp),
                                            )
                                            .padding(horizontal = 6.dp, vertical = 3.dp),
                                    )
                                }
                            },
                            onClick = { onEvent(RelationState.Event.AnimeSelected(item.animeId)) },
                        )
                    }
                }
            }
        }
    }
}

private fun previewStudioRelation() = AnimeRelation(
    title = "Kyoto Animation",
    anime = listOf(
        AnimeRelationItem(
            animeId = 1,
            title = "Violet Evergarden",
            posterUrl = null,
            rating = 8.7,
            year = 2018,
        ),
        AnimeRelationItem(
            animeId = 2,
            title = "K-On!",
            posterUrl = null,
            rating = 8.1,
            year = 2009,
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
