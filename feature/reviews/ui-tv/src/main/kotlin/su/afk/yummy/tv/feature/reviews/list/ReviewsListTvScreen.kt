package su.afk.yummy.tv.feature.reviews.list

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingFooter
import su.afk.yummy.tv.core.designsystem.presenter.components.loader.TvLoadingScreen
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvAppendErrorFooter
import su.afk.yummy.tv.core.designsystem.presenter.tv.TvStateMessage
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.feature.reviews.tv.R
import su.afk.yummy.tv.feature.reviews.view.ReviewTvCard

@Composable
fun ReviewsListTvScreen(
    state: ReviewsListState.State,
    effect: Flow<ReviewsListState.Effect>,
    onEvent: (ReviewsListState.Event) -> Unit
) {
    val context = LocalContext.current
    val reviews = state.reviews.collectAsLazyPagingItems()
    LaunchedEffect(effect) {
        effect.collect {
            if (it is ReviewsListState.Effect.ShowToast) {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(
                horizontal = TvScreenPadding.Horizontal,
                vertical = TvScreenPadding.Vertical,
            ),
    ) {
        val sortChipColors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            selectedContainerColor = MaterialTheme.colorScheme.primary,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
        )
        Text(
            stringResource(R.string.reviews_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Row(
            Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
        ) {
            state.availableSorts.forEach { sort ->
                FilterChip(
                    state.sort == sort,
                    { onEvent(ReviewsListState.Event.SortSelected(sort)) },
                    label = { Text(sort.label()) },
                    colors = sortChipColors,
                )
            }
        }

        val refresh = reviews.loadState.refresh
        when {
            refresh is LoadState.Loading -> TvLoadingScreen(Modifier.weight(1f))

            refresh is LoadState.Error -> Box(
                Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TvStateMessage(
                    title = stringResource(R.string.reviews_error),
                    icon = Icons.Filled.Warning,
                    onRetry = reviews::retry,
                )
            }

            reviews.itemCount == 0 -> Box(
                Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                TvStateMessage(
                    title = stringResource(R.string.reviews_empty),
                    icon = Icons.Filled.RateReview,
                )
            }

            else -> {
                val firstCardFocus = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { firstCardFocus.requestFocus() } }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(TvCardSpacing.Horizontal),
                    verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                    contentPadding = PaddingValues(
                        top = TvCardSpacing.Vertical,
                        bottom = TvScreenPadding.Vertical,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .tvFocusRestorer(fallback = firstCardFocus),
                ) {
                    items(
                        reviews.itemCount,
                        key = { index -> reviews[index]?.id ?: index }) { index ->
                        reviews[index]?.let { review ->
                            ReviewTvCard(
                                review = review,
                                reactions = state.reactionOverrides[review.id] ?: review.reactions,
                                showAnime = state.isGeneralFeed,
                                onOpen = { onEvent(ReviewsListState.Event.ReviewSelected(review.id)) },
                                modifier = if (index == 0) {
                                    Modifier.focusRequester(firstCardFocus)
                                } else {
                                    Modifier
                                },
                            )
                        }
                    }
                    when (reviews.loadState.append) {
                        is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                            TvLoadingFooter()
                        }

                        is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                            TvAppendErrorFooter(
                                message = stringResource(R.string.reviews_error),
                                onRetry = reviews::retry,
                            )
                        }

                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSort.label() = when (this) {
    ReviewSort.NEW -> stringResource(R.string.reviews_new)
    ReviewSort.OLD -> stringResource(R.string.reviews_old)
    ReviewSort.TOP -> stringResource(R.string.reviews_top)
}
