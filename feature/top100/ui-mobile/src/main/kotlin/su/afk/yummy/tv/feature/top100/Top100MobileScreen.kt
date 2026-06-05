package su.afk.yummy.tv.feature.top100

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.top100.mobile.R
import su.afk.yummy.tv.feature.top100.view.Top100MobileTypeTabs

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun Top100MobileScreen(
    state: Top100State.State,
    effect: Flow<Top100State.Effect>,
    onEvent: (Top100State.Event) -> Unit,
) {
    val initialError = state.error?.takeIf { state.items.isEmpty() }
    BaseScreen(
        isScroll = false,
        topBar = { Text(stringResource(R.string.top100_mobile_title)) },
        isLoading = state.isLoading && state.items.isEmpty(),
        error = initialError?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(Top100State.Event.RetrySelected) },
        errorContent = initialError?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.top100_mobile_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        MobilePosterGrid(contentPadding = PaddingValues()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Top100MobileTypeTabs(
                    selectedType = state.selectedType,
                    onTypeSelected = { onEvent(Top100State.Event.TypeSelected(it)) },
                )
            }

            if (state.error != null && state.items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Button(onClick = { onEvent(Top100State.Event.RetrySelected) }) {
                        Text(
                            stringResource(
                                R.string.top100_mobile_retry_error,
                                state.error.orEmpty()
                            )
                        )
                    }
                }
            }

            itemsIndexed(state.items, key = { _, item -> item.id }) { index, item ->
                MobilePosterCard(
                    title = item.title,
                    posterUrl = item.posterUrl,
                    rating = item.rating,
                    posterOverlay = {
                        Top100MobileRankBadge(
                            rank = index + 1,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(4.dp),
                        )
                    },
                    onClick = { onEvent(Top100State.Event.AnimeSelected(item.id)) },
                )
            }

            if (state.canLoadMore) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Button(
                        onClick = { onEvent(Top100State.Event.LoadMore) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.isLoadingMore) {
                                stringResource(R.string.top100_mobile_loading)
                            } else {
                                stringResource(R.string.top100_mobile_load_more)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Top100MobileRankBadge(
    rank: Int,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "#$rank",
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 2.dp),
    )
}
