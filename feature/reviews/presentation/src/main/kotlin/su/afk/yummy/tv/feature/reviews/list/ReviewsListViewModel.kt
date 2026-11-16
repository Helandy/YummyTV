package su.afk.yummy.tv.feature.reviews.list

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.plus
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.mutation.PendingMutation
import su.afk.yummy.tv.core.model.mutation.PendingMutationQueue
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.utils.paging.PagedSource
import su.afk.yummy.tv.core.utils.paging.pagingSource
import su.afk.yummy.tv.domain.reviews.model.AnimeReviewSummary
import su.afk.yummy.tv.domain.reviews.model.ReviewSort
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.repository.ReviewMutationRepository
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
    private val pendingMutationQueue: PendingMutationQueue,
    mutationNotifier: ReviewMutationRepository,
    settingsStore: YaniAccountSettingsStore,
) : BaseViewModel<ReviewsListState.State, ReviewsListState.Event, ReviewsListState.Effect>() {
    @AssistedFactory
    interface Factory {
        fun create(animeId: Int?): ReviewsListViewModel
    }

    private val sources = mutableMapOf<ReviewSort, PagedSource<AnimeReviewSummary>>()
    private val staleSorts = mutableSetOf<ReviewSort>()

    override fun createInitialState(): ReviewsListState.State {
        val isGeneralFeed = animeId == null
        val sorts = if (isGeneralFeed) listOf(ReviewSort.NEW, ReviewSort.TOP) else ReviewSort.entries
        // Pager холодный: источник не грузит, пока вкладку никто не собирает.
        val flows = sorts.associateWith { sourceFor(it).flow }.toPersistentMap()
        return ReviewsListState.State(
            reviews = flows.getValue(ReviewSort.NEW),
            reviewsBySort = flows,
            isGeneralFeed = isGeneralFeed,
        )
    }

    init {
        settingsStore.yaniUserId.onEach { setState { copy(currentUserId = it) } }
            .launchIn(viewModelScope)
        mutationNotifier.version.drop(1).onEach {
            // Свежая страница уже несёт актуальные счётчики (кэш инвалидирован при мутации),
            // поэтому сбрасываем накопленные оптимистичные override, чтобы они не маскировали
            // серверное состояние и не росли безгранично.
            setState { copy(reactionOverrides = persistentMapOf()) }
            // Активную вкладку перезагружаем сразу, остальные — когда их выберут.
            staleSorts.addAll(sources.keys - currentState.sort)
            sources[currentState.sort]?.invalidate()
        }.launchIn(viewModelScope)
    }

    override fun onEvent(event: ReviewsListState.Event) {
        when (event) {
            ReviewsListState.Event.BackSelected -> nav.back()
            is ReviewsListState.Event.ReviewSelected -> nav.navigateDetail(navigator.details(event.id))
            is ReviewsListState.Event.AuthorSelected -> nav.navigate(
                accountNavigator.getUserProfileDest(
                    event.userId
                )
            )

            is ReviewsListState.Event.SortSelected -> if (event.sort != currentState.sort) {
                if (staleSorts.remove(event.sort)) sources[event.sort]?.invalidate()
                setState {
                    copy(
                        sort = event.sort,
                        reviews = reviewsBySort[event.sort] ?: sourceFor(event.sort).flow,
                        reactionOverrides = persistentMapOf(),
                    )
                }
            }

            is ReviewsListState.Event.VoteSelected -> vote(event.review, event.vote)
        }
    }

    private fun sourceFor(sort: ReviewSort) = sources.getOrPut(sort) {
        pagingSource(viewModelScope, itemKey = { it.id }) { limit, offset ->
            val page = animeId?.let { getAnimeReviews(it, sort, limit, offset) }
                ?: getReviewFeed(sort, limit, offset)
            page.reviews
        }
    }

    private fun vote(review: AnimeReviewSummary, target: ReviewVote) {
        if (!currentState.isSignedIn) {
            toast(strings.get(R.string.reviews_auth_required)); return
        }
        val old = currentState.reactionOverrides[review.id] ?: review.reactions
        val optimistic = old.optimistic(target)
        setState { copy(reactionOverrides = reactionOverrides + (review.id to optimistic)) }
        viewModelScope.launch {
            runSuspendCatching { voteReview(review.id, target) }.fold(
                { saved -> setState { copy(reactionOverrides = reactionOverrides + (review.id to saved)) } },
                { error ->
                    // Офлайн: оптимистичная реакция остаётся, мутация уйдёт из очереди сама.
                    val mutation = PendingMutation.VoteReview(review.id, target.apiValue)
                    if (!pendingMutationQueue.enqueueOnNetworkFailure(mutation, error)) {
                        setState { copy(reactionOverrides = reactionOverrides + (review.id to old)) }
                        toast(strings.get(R.string.reviews_vote_error))
                    }
                },
            )
        }
    }

    private fun toast(message: String) = setEffect(ReviewsListState.Effect.ShowToast(message))
}
