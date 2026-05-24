package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.AnimeRatingSummary
import su.afk.yummy.tv.domain.account.UserAnimeList
import su.afk.yummy.tv.domain.anime.AnimeDetails

@Composable
internal fun DetailsContent(
    details: AnimeDetails,
    isWatchLoading: Boolean,
    watchProgress: Map<String, WatchProgressEntry>,
    isInLibrary: Boolean,
    libraryList: UserAnimeList?,
    ratingSummary: AnimeRatingSummary,
    collections: List<AnimeCollectionSummary>,
    selectedUserRating: Int?,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingScreenSelected: () -> Unit,
    onCollectionSelected: (Int) -> Unit,
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
            libraryList = libraryList,
            isWatchLoading = isWatchLoading,
            watchProgress = watchProgress,
            ratingSummary = ratingSummary,
            collections = collections,
            selectedUserRating = selectedUserRating,
            onWatchSelected = onWatchSelected,
            onLibraryToggle = onLibraryToggle,
            onFullDetailsSelected = onFullDetailsSelected,
            onEpisodesSelected = onEpisodesSelected,
            onTrailersSelected = onTrailersSelected,
            onSimilarSelected = onSimilarSelected,
            onViewingOrderSelected = onViewingOrderSelected,
            onScreenshotsSelected = onScreenshotsSelected,
            onRatingScreenSelected = onRatingScreenSelected,
            onCollectionSelected = onCollectionSelected,
        )
    }
}
