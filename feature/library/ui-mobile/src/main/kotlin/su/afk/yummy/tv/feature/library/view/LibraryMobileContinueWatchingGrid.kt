package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.mobile.MobileBottomBarDefaults
import su.afk.yummy.tv.core.preferences.settings.LibraryContinueWatchingCardSize
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem
import su.afk.yummy.tv.feature.library.mobile.R

@Composable
internal fun LibraryMobileContinueWatchingGrid(
    entries: List<HomeContinueWatchingItem>,
    cardSize: LibraryContinueWatchingCardSize,
    showRemoteLoader: Boolean,
    isEmpty: Boolean,
    remoteError: String?,
    isRemoteLoading: Boolean,
    onRetry: () -> Unit,
    onEntrySelected: (HomeContinueWatchingItem) -> Unit,
    onDetailsSelected: (HomeContinueWatchingItem) -> Unit,
    onDeleteSelected: (HomeContinueWatchingItem) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(cardSize.mobileGridMinWidth),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            top = 12.dp,
            end = 16.dp,
            bottom = MobileBottomBarDefaults.ContentBottomPadding +
                    MobileBottomBarDefaults.ExtraContentBottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showRemoteLoader) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryMobileLoadingIndicator()
            }
        }

        if (isEmpty && !showRemoteLoader) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryMobileEmptyList()
            }
        }

        items(
            items = entries,
            key = { entry -> "${entry.animeId}-${entry.episode}" },
        ) { entry ->
            val episodeLabel = stringResource(
                R.string.library_mobile_episode,
                entry.episode.ifBlank { "?" },
            )
            LibraryMobileContinueWatchingCard(
                entry = entry,
                episodeLabel = episodeLabel,
                modifier = Modifier.fillMaxWidth(),
                onDetails = { onDetailsSelected(entry) },
                onDelete = { onDeleteSelected(entry) },
                onClick = { onEntrySelected(entry) },
            )
        }

        remoteError?.let { message ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                LibraryMobileRemoteError(
                    message = message,
                    isLoading = isRemoteLoading,
                    onRetry = onRetry,
                )
            }
        }
    }
}

private val LibraryContinueWatchingCardSize.mobileGridMinWidth: Dp
    get() = when (this) {
        LibraryContinueWatchingCardSize.COMPACT -> 156.dp
        LibraryContinueWatchingCardSize.STANDARD -> 220.dp
        LibraryContinueWatchingCardSize.LARGE -> 320.dp
    }
