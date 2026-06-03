package su.afk.yummy.tv.feature.details.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.anime.model.AnimeDetails

@Composable
internal fun DetailsContent(
    details: AnimeDetails,
    videosState: VideosUiState,
    isWatchLoading: Boolean,
    watchProgress: Map<String, WatchProgressEntry>,
    isInLibrary: Boolean,
    isFavorite: Boolean,
    libraryList: UserAnimeList?,
    collections: List<AnimeCollectionSummary>,
    canSubscribe: Boolean,
    detailsButtonOrder: List<DetailsButtonAction>,
    restoreButtonFocusRequest: Int,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSubscriptionsSelected: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingScreenSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
) {
    val barFocusRequester = remember { FocusRequester() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        DetailsHero(
            details = details,
            downFocusRequester = barFocusRequester,
            isInLibrary = isInLibrary,
            isFavorite = isFavorite,
            libraryList = libraryList,
            videosState = videosState,
            isWatchLoading = isWatchLoading,
            watchProgress = watchProgress,
            collections = collections,
            canSubscribe = canSubscribe,
            detailsButtonOrder = detailsButtonOrder,
            restoreButtonFocusRequest = restoreButtonFocusRequest,
            onWatchSelected = onWatchSelected,
            onLibraryToggle = onLibraryToggle,
            onFavoriteToggle = onFavoriteToggle,
            onSubscriptionsSelected = onSubscriptionsSelected,
            onFullDetailsSelected = onFullDetailsSelected,
            onEpisodesSelected = onEpisodesSelected,
            onTrailersSelected = onTrailersSelected,
            onSimilarSelected = onSimilarSelected,
            onViewingOrderSelected = onViewingOrderSelected,
            onScreenshotsSelected = onScreenshotsSelected,
            onRatingScreenSelected = onRatingScreenSelected,
            onCollectionsSelected = onCollectionsSelected,
        )
    }
}
