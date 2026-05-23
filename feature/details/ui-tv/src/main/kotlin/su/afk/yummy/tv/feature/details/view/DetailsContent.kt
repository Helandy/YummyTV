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
import su.afk.yummy.tv.domain.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.VideosUiState

@Composable
internal fun DetailsContent(
    details: AnimeDetails,
    videosState: VideosUiState,
    watchProgress: Map<String, WatchProgressEntry>,
    isInLibrary: Boolean,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
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
            watchProgress = watchProgress,
            onWatchSelected = onWatchSelected,
            onLibraryToggle = onLibraryToggle,
            onFullDetailsSelected = onFullDetailsSelected,
            onEpisodesSelected = onEpisodesSelected,
            onTrailersSelected = onTrailersSelected,
            onSimilarSelected = onSimilarSelected,
            onViewingOrderSelected = onViewingOrderSelected,
            onScreenshotsSelected = onScreenshotsSelected,
        )
    }
}
