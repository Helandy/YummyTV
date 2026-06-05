package su.afk.yummy.tv.feature.details.similar

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.feature.details.details.SimilarUiState
import su.afk.yummy.tv.feature.details.mobile.R
import su.afk.yummy.tv.feature.details.similar.utils.bestUrl

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SimilarMobileScreen(

    state: SimilarState.State,
    effect: Flow<SimilarState.Effect>,
    onEvent: (SimilarState.Event) -> Unit,

) {
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = stringResource(R.string.details_mobile_similar),
                onBack = { onEvent(SimilarState.Event.BackSelected) },
            )
        },
    ) {
        MobilePosterGrid(contentPadding = PaddingValues(bottom = 8.dp)) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.fromAi) {
                        OutlinedButton(
                            onClick = { onEvent(SimilarState.Event.SourceToggled) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.details_mobile_similar_source_site))
                        }
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.details_mobile_similar_source_ai))
                        }
                    } else {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.details_mobile_similar_source_site))
                        }
                        OutlinedButton(
                            onClick = { onEvent(SimilarState.Event.SourceToggled) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(stringResource(R.string.details_mobile_similar_source_ai))
                        }
                    }
                }
            }
            when (val similar = state.similarState) {
                SimilarUiState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.details_mobile_loading))
                }
                SimilarUiState.Empty -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(stringResource(R.string.details_mobile_similar_empty))
                }
                is SimilarUiState.Content -> {
                    items(similar.items, key = { it.animeId }) { item ->
                        MobilePosterCard(
                            title = item.title,
                            posterUrl = item.poster.bestUrl(),
                            rating = item.rating,
                            onClick = { onEvent(SimilarState.Event.AnimeSelected(item.animeId)) },
                        )
                    }
                }
            }
        }
    }
}
