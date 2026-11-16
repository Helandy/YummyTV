package su.afk.yummy.tv.feature.reviews.list

import androidx.compose.runtime.Immutable
import androidx.paging.PagingData
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote

class ReviewsListState {
    @Immutable
    data class State(
        /** Поток текущей сортировки (TV-экран); мобильный пейджер берёт свой поток из [reviewsBySort]. */
        val reviews: Flow<PagingData<AnimeReviewSummary>> = flowOf(PagingData.empty()),
        /** Отдельный кэшируемый поток на каждую сортировку: соседняя вкладка видна во время свайпа. */
        val reviewsBySort: PersistentMap<ReviewSort, Flow<PagingData<AnimeReviewSummary>>> = persistentMapOf(),
        val sort: ReviewSort = ReviewSort.NEW,
        val currentUserId: Int = 0,
        val reactionOverrides: PersistentMap<Int, ReviewReactions> = persistentMapOf(),
        val isGeneralFeed: Boolean = false,
    ) : UiState {
        val isSignedIn get() = currentUserId > 0
        val availableSorts: ImmutableList<ReviewSort>
            get() = if (isGeneralFeed) listOf(
                ReviewSort.NEW,
                ReviewSort.TOP
            ).toImmutableList() else ReviewSort.entries.toImmutableList()
    }

    sealed interface Event : UiEvent {
        data object BackSelected : Event
        data class ReviewSelected(val id: Int) : Event
        data class AuthorSelected(val userId: Int) : Event
        data class SortSelected(val sort: ReviewSort) : Event
        data class VoteSelected(val review: AnimeReviewSummary, val vote: ReviewVote) : Event
    }

    sealed interface Effect : UiEffect {
        data class ShowToast(val message: String) : Effect
    }
}
