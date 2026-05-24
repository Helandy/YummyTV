package su.afk.yummy.tv.feature.details

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.account.AnimeCollectionSummary
import su.afk.yummy.tv.domain.account.UserAnimeList
import su.afk.yummy.tv.domain.anime.AnimeDetails
import su.afk.yummy.tv.domain.anime.AnimeRecommendation
import su.afk.yummy.tv.domain.anime.AnimeVideo

data class BalancerOption(val playerName: String, val video: AnimeVideo, val isSupported: Boolean = true)
data class BalancerPickerState(val episodeNumber: String, val options: List<BalancerOption>)

class DetailsState {
    data class State(
        val isLoading: Boolean = true,
        val details: AnimeDetails? = null,
        val videosState: VideosUiState = VideosUiState.Loading,
        val error: String? = null,
        val isInLibrary: Boolean = false,
        val libraryList: UserAnimeList? = null,
        val showPosterFullscreen: Boolean = false,
        val watchProgress: Map<String, WatchProgressEntry> = emptyMap(),
        val pendingBalancerSelection: BalancerPickerState? = null,
        val showLibraryListPicker: Boolean = false,
        val isWatchLaunchPending: Boolean = false,
        val collections: List<AnimeCollectionSummary> = emptyList(),
        val isSignedIn: Boolean = false,
        val subscriptionVideoId: Int = 0,
        val isSubscribed: Boolean = false,
    ) : UiState

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data object RetrySelected : Event
        data object WatchSelected : Event
        data class AnimeSelected(val seriesId: Int) : Event
        data object FullDetailsSelected : Event
        data object EpisodesSelected : Event
        data object TrailersSelected : Event
        data object SimilarSelected : Event
        data object ViewingOrderSelected : Event
        data object ScreenshotsSelected : Event
        data object RatingScreenSelected : Event
        data object LibraryToggled : Event
        data object LibraryListPickerDismissed : Event
        data class LibraryListSelected(val list: UserAnimeList) : Event
        data object PosterClicked : Event
        data object PosterDismissed : Event
        data object BalancerPickerDismissed : Event
        data class BalancerConfirmed(val video: AnimeVideo) : Event
        data class CollectionSelected(val collectionId: Int) : Event
        data object SubscriptionToggled : Event
    }

    sealed interface Effect : UiEffect
}

sealed interface VideosUiState {
    data object Loading : VideosUiState
    data object Empty : VideosUiState
    data class Content(val videos: List<AnimeVideo>) : VideosUiState
}

sealed interface SimilarUiState {
    data object Loading : SimilarUiState
    data object Empty : SimilarUiState
    data class Content(val items: List<AnimeRecommendation>) : SimilarUiState
}
