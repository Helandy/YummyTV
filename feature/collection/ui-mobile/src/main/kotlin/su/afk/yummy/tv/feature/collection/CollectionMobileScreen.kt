package su.afk.yummy.tv.feature.collection

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.baseScreen.BaseScreen
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileMessage
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobilePosterGrid
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileTopBar
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.feature.collection.mobile.R
import su.afk.yummy.tv.feature.collection.view.CollectionMobileHeader

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun CollectionMobileScreen(
    state: CollectionState.State,
    effect: Flow<CollectionState.Effect>,
    onEvent: (CollectionState.Event) -> Unit,
) {
    val collection = state.collection
    BaseScreen(
        isScroll = false,
        customTopBar = {
            MobileTopBar(
                title = collection?.title ?: stringResource(R.string.collection_mobile_title),
                onBack = { onEvent(CollectionState.Event.BackSelected) },
            )
        },
        isLoading = state.isLoading,
        error = state.error?.let { ErrorItem(title = it, message = it) },
        onRetry = { onEvent(CollectionState.Event.RetrySelected) },
        isEmpty = collection?.animes.orEmpty().isEmpty() && !state.isLoading,
        errorContent = state.error?.let { message ->
            { _, retry ->
                MobileMessage(
                    title = message,
                    actionLabel = stringResource(R.string.collection_mobile_retry),
                    onAction = retry,
                )
            }
        },
    ) {
        MobilePosterGrid(contentPadding = PaddingValues()) {
            if (collection != null) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    CollectionMobileHeader(collection = collection)
                }
                items(collection.animes, key = { it.id }) { item ->
                    MobilePosterCard(
                        title = item.title,
                        posterUrl = item.posterUrl,
                        rating = item.rating,
                        onClick = { onEvent(CollectionState.Event.AnimeSelected(item.id)) },
                    )
                }
            }
        }
    }
}
