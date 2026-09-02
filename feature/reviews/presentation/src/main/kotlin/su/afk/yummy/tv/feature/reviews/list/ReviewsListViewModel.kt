package su.afk.yummy.tv.feature.reviews.list

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.plus
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.error.api.isNetworkError
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.outbox.PendingMutationOutbox
import su.afk.yummy.tv.core.storage.outbox.PendingMutationSyncScheduler
import su.afk.yummy.tv.core.storage.outbox.PendingMutationTypes
import su.afk.yummy.tv.core.storage.outbox.VoteReviewPayload
import su.afk.yummy.tv.core.utils.paging.PagedSource
import su.afk.yummy.tv.core.utils.paging.pagingSource
import su.afk.yummy.tv.domain.reviews.ReviewMutationNotifier
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
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
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val navigator: IReviewsNavigator,
    private val accountNavigator: IAccountNavigator,
    private val getReviewFeed: GetReviewFeedUseCase,
    private val getAnimeReviews: GetAnimeReviewsUseCase,
    private val voteReview: VoteReviewUseCase,
    private val strings: StringProvider,
    private val pendingMutationOutbox: PendingMutationOutbox,
    private val pendingMutationSyncScheduler: PendingMutationSyncScheduler,
    mutationNotifier: ReviewMutationNotifier,
    settingsStore: YaniAccountSettingsStore,
) : BaseViewModel<ReviewsListState.State, ReviewsListState.Event, ReviewsListState.Effect>() {
    @AssistedFactory
    interface Factory {
        fun create(animeId: Int?): ReviewsListViewModel
    }

    private var pagedSource: PagedSource<AnimeReviewSummary>? = null
    override fun createInitialState() = ReviewsListState.State(
        reviews = createFlow(ReviewSort.NEW),
        isGeneralFeed = animeId == null,
    )

    init {
        settingsStore.yaniUserId.onEach { setState { copy(currentUserId = it) } }
            .launchIn(viewModelScope)
        mutationNotifier.version.drop(1).onEach {
            // Свежая страница уже несёт актуальные счётчики (кэш инвалидирован при мутации),
            // поэтому сбрасываем накопленные оптимистичные override, чтобы они не маскировали
            // серверное состояние и не росли безгранично.
            setState { copy(reactionOverrides = persistentMapOf()) }
            pagedSource?.invalidate()
        }.launchIn(viewModelScope)
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
                    reactionOverrides = persistentMapOf()
                )
            }

            is ReviewsListState.Event.VoteSelected -> vote(event.review, event.vote)
        }
    }

    private fun createFlow(sort: ReviewSort) =
        pagingSource(viewModelScope) { limit, offset ->
            val page = animeId?.let { getAnimeReviews(it, sort, limit, offset) }
                ?: getReviewFeed(sort, limit, offset)
            page.reviews
        }.also { pagedSource = it }.flow

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
                { error ->
                    if (error.isNetworkError()) {
                        // Офлайн: оптимистичная реакция остаётся, мутация уйдёт из очереди сама.
                        pendingMutationOutbox.enqueue(
                            PendingMutationTypes.VOTE_REVIEW,
                            VoteReviewPayload(review.id, target.apiValue).encode(),
                        )
                        pendingMutationSyncScheduler.scheduleFlush()
                    } else {
                        setState { copy(reactionOverrides = reactionOverrides + (review.id to old)) }
                        toast(strings.get(R.string.reviews_vote_error))
                    }
                },
            )
        }
    }

    private fun toast(message: String) = setEffect(ReviewsListState.Effect.ShowToast(message))
}
