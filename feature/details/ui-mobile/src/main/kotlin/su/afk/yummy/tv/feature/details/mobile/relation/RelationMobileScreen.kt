package su.afk.yummy.tv.feature.details.mobile.relation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
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
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.relation.RelationState

@Preview(name = "Default", device = "spec:width=412dp,height=915dp,dpi=420", showBackground = true)
@Composable
private fun RelationMobileScreenPreview() = ScreenPreviewTheme {
    RelationMobileScreen(RelationState.State(isLoading = false), emptyFlow()) {}
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
                title = state.relation?.title.orEmpty(),
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
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        relation.secondaryTitle?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        relation.description?.let {
                            Text(text = it, style = MaterialTheme.typography.bodyLarge)
                        }
                        relation.subGenres.forEach { genre ->
                            AssistChip(
                                onClick = {
                                    onEvent(RelationState.Event.SubGenreSelected(genre.id))
                                },
                                label = { Text(genre.title) },
                            )
                        }
                        Text(
                            text = stringResource(R.string.details_mobile_related_anime),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        if (relation.anime.isEmpty()) {
                            Text(
                                text = stringResource(R.string.details_mobile_related_empty),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
