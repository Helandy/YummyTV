package su.afk.yummy.tv.feature.home

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.home.model.HomeFeed

class HomeState {
    data class State(
        val isLoading: Boolean = true,
        val feed: HomeFeed? = null,
        val error: String? = null,
        val focusedItemId: Int? = null,
        val restoreFocusedItemOnEnter: Boolean = false,
        val focusedSectionId: String? = null,
        val focusedPreview: AnimePreview? = null,
        val animePreviews: Map<Int, AnimePreview> = emptyMap(),
        val continueWatching: List<WatchProgressEntry> = emptyList(),
        val isContinueWatchingLoaded: Boolean = false,
        val continueWatchingRestoreToken: Int = 0,
    ) : UiState

    sealed interface Event : UiEvent {
        data class AnimeSelected(
            val seriesId: Int,
            val sourceSectionId: String? = null,
            val displayId: Int? = null,
        ) : Event
        data class VideoSelected(val videoId: Int) : Event
        data class CollectionSelected(
            val collectionId: Int,
            val sourceSectionId: String? = null,
            val displayId: Int? = null,
        ) : Event

        data class ItemFocused(val sectionId: String, val displayId: Int, val animeId: Int?) : Event
        data class HeroItemVisible(val displayId: Int) : Event
        data class ContinueWatchingSelected(val entry: WatchProgressEntry) : Event
        data object FocusedItemRestoreHandled : Event
        data object RetrySelected : Event
    }

    sealed interface Effect : UiEffect
}
