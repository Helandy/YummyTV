package su.afk.yummy.tv.feature.account.mysubscriptions

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.focus.rememberTvGridStartExtent
import su.afk.yummy.tv.core.designsystem.focus.rememberTvTopAnchoredGridBringIntoViewSpec
import su.afk.yummy.tv.core.designsystem.focus.tvLazyGridRowFocusNavigation
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.designsystem.tv.TvStateContent
import su.afk.yummy.tv.core.designsystem.tv.TvTitleCard
import su.afk.yummy.tv.domain.account.model.SubscriptionKeys
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.utils.accountErrorMessage

@Composable
fun MySubscriptionsTvScreen(
    state: MySubscriptionsState.State,
    effect: Flow<MySubscriptionsState.Effect>,
    onEvent: (MySubscriptionsState.Event) -> Unit,
) {
    LaunchedEffect(Unit) { onEvent(MySubscriptionsState.Event.ScreenShown) }

    TvStateContent(
        isLoading = state.isLoading && state.subscriptions.isEmpty(),
        error = state.error.accountErrorMessage(),
        empty = state.subscriptions.isEmpty(),
        emptyText = stringResource(
            if (state.isSignedIn) {
                R.string.account_my_subscriptions_empty
            } else {
                R.string.account_my_subscriptions_signed_out
            }
        ),
        onRetry = { onEvent(MySubscriptionsState.Event.RetrySelected) },
    ) {
        MySubscriptionsGrid(state = state, onEvent = onEvent)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MySubscriptionsGrid(
    state: MySubscriptionsState.State,
    onEvent: (MySubscriptionsState.Event) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    val cardWidth = currentTvTitleCardDimensions().width
    val itemCount = state.subscriptions.size
    val itemFocusRequesters = remember(itemCount) { List(itemCount) { FocusRequester() } }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val horizontalSpacing = TvCardSpacing.Horizontal
        val columnCount =
            (((maxWidth - TvScreenPadding.Horizontal - TvScreenPadding.Horizontal).value + horizontalSpacing.value) /
                    (cardWidth.value + horizontalSpacing.value)).toInt()
                .coerceAtLeast(1)

        val gridStartExtent = rememberTvGridStartExtent(TvScreenPadding.Vertical, TvCardSpacing.Vertical)

        CompositionLocalProvider(LocalBringIntoViewSpec provides rememberTvTopAnchoredGridBringIntoViewSpec(TvCardSpacing.Vertical)) {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Adaptive(minSize = cardWidth),
                contentPadding = PaddingValues(
                    start = TvScreenPadding.Horizontal,
                    end = TvScreenPadding.Horizontal,
                    top = TvScreenPadding.Vertical,
                    bottom = TvScreenPadding.Vertical,
                ),
                verticalArrangement = Arrangement.spacedBy(TvCardSpacing.Vertical),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
                modifier = Modifier
                    .fillMaxSize()
                    .focusGroup(),
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = stringResource(R.string.account_my_subscriptions),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = gridStartExtent.measure,
                    )
                }

                itemsIndexedSubscriptions(state, onEvent, itemFocusRequesters) { index, modifier ->
                    modifier
                        .tvWholeItemBringIntoView(gridStartExtent.takeIf { index < columnCount })
                        .tvLazyGridRowFocusNavigation(
                            index = index,
                            columnCount = columnCount,
                            itemCount = itemCount,
                            gridState = gridState,
                            scope = scope,
                            focusRequesterAt = itemFocusRequesters::getOrNull,
                            // нулевой lazy-индекс занимает заголовок экрана
                            lazyIndexOffset = 1,
                        )
                }
            }
        }
    }
}

private inline fun androidx.compose.foundation.lazy.grid.LazyGridScope.itemsIndexedSubscriptions(
    state: MySubscriptionsState.State,
    noinline onEvent: (MySubscriptionsState.Event) -> Unit,
    itemFocusRequesters: List<FocusRequester>,
    crossinline focusNavigation: (Int, Modifier) -> Modifier,
) {
    items(
        count = state.subscriptions.size,
        key = { index ->
            val item = state.subscriptions[index]
            SubscriptionKeys.animePlayerKey(item.animeId, item.playerId, item.player)
        },
    ) { index ->
        val subscription = state.subscriptions[index]
        TvTitleCard(
            title = subscription.title,
            posterUrl = subscription.posterUrl,
            subtitle = subscription.player.takeIf { it.isNotBlank() },
            onClick = {
                onEvent(MySubscriptionsState.Event.SubscriptionSelected(subscription.animeId))
            },
            modifier = focusNavigation(
                index,
                Modifier.focusRequester(itemFocusRequesters[index]),
            ),
        )
    }
}
