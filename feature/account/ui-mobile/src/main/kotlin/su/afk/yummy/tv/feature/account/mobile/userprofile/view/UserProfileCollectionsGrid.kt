package su.afk.yummy.tv.feature.account.mobile.userprofile.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import su.afk.yummy.tv.core.designsystem.R as CoreR
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator
import su.afk.yummy.tv.core.designsystem.error.uiMessage
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterCard
import su.afk.yummy.tv.core.designsystem.mobile.cards.MobilePosterGrid
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileEmptyText
import su.afk.yummy.tv.feature.account.mobile.view.AccountMobileLoadingIndicator
import su.afk.yummy.tv.feature.account.presentation.R
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

/** Коллекции пользователя сеткой постеров — как в каталоге коллекций. */
@Composable
internal fun UserProfileCollectionsGrid(
    collections: LazyPagingItems<AnimeCollectionSummary>,
    onEvent: (UserProfileState.Event) -> Unit,
) {
    val onRetry = {
        onEvent(UserProfileState.Event.RetryTabSelected)
        collections.retry()
    }
    val refresh = collections.loadState.refresh
    val append = collections.loadState.append
    MobilePosterGrid(contentPadding = PaddingValues(bottom = 32.dp)) {
        when {
            refresh is LoadState.Loading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                AccountMobileLoadingIndicator()
            }

            refresh is LoadState.Error && collections.itemCount == 0 ->
                item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                    UserProfileMessage(
                        text = refresh.error.uiMessage(),
                        action = stringResource(CoreR.string.common_retry),
                        onAction = onRetry,
                    )
                }

            collections.itemCount == 0 -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                AccountMobileEmptyText(text = stringResource(R.string.user_profile_section_empty))
            }
        }
        items(
            count = collections.itemCount,
            key = collections.itemKey { "collection_${it.id}" },
        ) { index ->
            collections[index]?.let { item ->
                MobilePosterCard(
                    title = item.title,
                    posterUrl = item.posterUrl,
                    onClick = { onEvent(UserProfileState.Event.CollectionSelected(item.id)) },
                )
            }
        }
        when (append) {
            is LoadState.Loading -> item(key = "loading_more", span = { GridItemSpan(maxLineSpan) }) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    YummyLoadingIndicator()
                }
            }

            is LoadState.Error -> item(key = "append_error", span = { GridItemSpan(maxLineSpan) }) {
                UserProfileMessage(
                    text = append.error.uiMessage(),
                    action = stringResource(CoreR.string.common_retry),
                    onAction = onRetry,
                )
            }

            is LoadState.NotLoading -> Unit
        }
    }
}
