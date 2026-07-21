package su.afk.yummy.tv.feature.reviews.list

import androidx.lifecycle.SavedStateHandle
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.OffsetPage
import su.afk.yummy.tv.core.utils.OffsetPagingSource
import su.afk.yummy.tv.domain.reviews.ReviewMutationNotifier
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.usecase.GetAnimeReviewsUseCase
import su.afk.yummy.tv.domain.reviews.usecase.GetReviewFeedUseCase
import su.afk.yummy.tv.domain.reviews.usecase.VoteReviewUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.reviews.presentation.R

@HiltViewModel(assistedFactory = ReviewsListViewModel.Factory::class)
class ReviewsListViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int?,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val navigator: IReviewsNavigator,
    private val accountNavigator: IAccountNavigator,
    private val getReviewFeed: GetReviewFeedUseCase,
    private val getAnimeReviews: GetAnimeReviewsUseCase,
    private val voteReview: VoteReviewUseCase,
    private val strings: StringProvider,
    mutationNotifier: ReviewMutationNotifier,
    settingsStore: SettingsStore,
) : BaseViewModelNew<ReviewsListState.State, ReviewsListState.Event, ReviewsListState.Effect>(
    savedStateHandle
) {
    @AssistedFactory
    interface Factory {
        fun create(animeId: Int?): ReviewsListViewModel
    }

    private var pagingSource: PagingSource<Int, AnimeReviewSummary>? = null
    override fun createInitialState() = ReviewsListState.State(
        reviews = createFlow(ReviewSort.NEW),
        isGeneralFeed = animeId == null,
    )

    init {
        settingsStore.yaniUserId.onEach { setState { copy(currentUserId = it) } }
            .launchIn(viewModelScope)
        mutationNotifier.version.drop(1).onEach { pagingSource?.invalidate() }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ReviewsListState.Event) {
        when (event) {
            ReviewsListState.Event.BackSelected -> nav.back()
            is ReviewsListState.Event.ReviewSelected -> nav.navigate(navigator.details(event.id))
            is ReviewsListState.Event.AuthorSelected -> nav.navigate(
                accountNavigator.getUserProfileDest(
                    event.userId
                )
            )

            is ReviewsListState.Event.SortSelected -> if (event.sort != currentState.sort) setState {
                copy(
                    sort = event.sort,
                    reviews = createFlow(event.sort),
                    reactionOverrides = emptyMap()
                )
            }

            is ReviewsListState.Event.VoteSelected -> vote(event.review, event.vote)
        }
    }

    private fun createFlow(sort: ReviewSort) =
        Pager(PagingConfig(pageSize = 20, initialLoadSize = 20, enablePlaceholders = false)) {
            OffsetPagingSource { limit, offset ->
                val pageLimit = limit.coerceAtMost(20)
                val page = animeId?.let { getAnimeReviews(it, sort, pageLimit, offset) }
                    ?: getReviewFeed(sort, pageLimit, offset)
                OffsetPage(
                    page.reviews,
                    offset + page.reviews.size,
                    page.reviews.size >= pageLimit
                )
            }.also { pagingSource = it }
        }.flow.cachedIn(viewModelScope)

    private fun vote(review: AnimeReviewSummary, target: ReviewVote) {
        if (!currentState.isSignedIn) {
            toast(strings.get(R.string.reviews_auth_required)); return
        }
        val old = currentState.reactionOverrides[review.id] ?: review.reactions
        val optimistic = old.optimistic(target)
        setState { copy(reactionOverrides = reactionOverrides + (review.id to optimistic)) }
        viewModelScope.launch {
            runCatching { voteReview(review.id, target) }.fold(
                { saved -> setState { copy(reactionOverrides = reactionOverrides + (review.id to saved)) } },
                {
                    setState { copy(reactionOverrides = reactionOverrides + (review.id to old)) }; toast(
                    it.message ?: strings.get(R.string.reviews_vote_error)
                )
                },
            )
        }
    }

    private fun ReviewReactions.optimistic(target: ReviewVote): ReviewReactions {
        var nextLikes = likes - if (vote == ReviewVote.LIKE) 1 else 0
        var nextDislikes = dislikes - if (vote == ReviewVote.DISLIKE) 1 else 0
        if (target == ReviewVote.LIKE) nextLikes++
        if (target == ReviewVote.DISLIKE) nextDislikes++
        return copy(
            likes = nextLikes.coerceAtLeast(0),
            dislikes = nextDislikes.coerceAtLeast(0),
            vote = target
        )
    }

    private fun toast(message: String) = setEffect(ReviewsListState.Effect.ShowToast(message))
}
