package su.afk.yummy.tv.feature.reviews.details

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.domain.reviews.model.ReviewReactions
import su.afk.yummy.tv.domain.reviews.model.ReviewVote
import su.afk.yummy.tv.domain.reviews.usecase.DeleteReviewUseCase
import su.afk.yummy.tv.domain.reviews.usecase.GetReviewDetailsUseCase
import su.afk.yummy.tv.domain.reviews.usecase.VoteReviewUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.reviews.presentation.R

@HiltViewModel(assistedFactory = ReviewDetailsViewModel.Factory::class)
class ReviewDetailsViewModel @AssistedInject constructor(
    @Assisted private val reviewId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val accountNavigator: IAccountNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val detailsNavigator: IDetailsNavigator,
    private val getReviewDetails: GetReviewDetailsUseCase,
    private val voteReview: VoteReviewUseCase,
    private val deleteReview: DeleteReviewUseCase,
    private val strings: StringProvider,
    settingsStore: SettingsStore,
) : BaseViewModelNew<ReviewDetailsState.State, ReviewDetailsState.Event, ReviewDetailsState.Effect>(
    savedStateHandle
) {
    @AssistedFactory
    interface Factory {
        fun create(reviewId: Int): ReviewDetailsViewModel
    }

    override fun createInitialState() = ReviewDetailsState.State()

    init {
        settingsStore.yaniUserId.onEach { setState { copy(currentUserId = it) } }
            .launchIn(viewModelScope); load()
    }

    override fun onEvent(event: ReviewDetailsState.Event) {
        when (event) {
            ReviewDetailsState.Event.BackSelected -> nav.back()
            ReviewDetailsState.Event.RetrySelected -> load()
            ReviewDetailsState.Event.DeleteConfirmed -> delete()
            is ReviewDetailsState.Event.VoteSelected -> vote(event.vote)
            is ReviewDetailsState.Event.AuthorSelected -> nav.navigate(
                accountNavigator.getUserProfileDest(
                    event.userId
                )
            )

            is ReviewDetailsState.Event.AnimeSelected -> nav.navigate(
                detailsNavigator.getDetailsDest(event.animeId)
            )

            ReviewDetailsState.Event.CommentsSelected -> nav.navigate(
                commentsNavigator.getCommentsDest(CommentTargetType.REVIEW, reviewId)
            )
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState {
                copy(
                    loading = true,
                    error = null
                )
            }; runCatching { getReviewDetails(reviewId) }.fold({
            setState {
                copy(
                    loading = false,
                    details = it
                )
            }
        }, { setState { copy(loading = false, error = it.message) } })
        }
    }

    private fun vote(vote: ReviewVote) {
        if (currentState.currentUserId <= 0) {
            toast(strings.get(R.string.reviews_auth_required)); return
        }
        val old = currentState.details ?: return
        val optimistic =
            old.copy(review = old.review.copy(reactions = old.review.reactions.optimistic(vote)))
        setState { copy(details = optimistic) }
        viewModelScope.launch {
            runCatching { voteReview(reviewId, vote) }.fold(
                { saved ->
                    setState {
                        copy(details = details?.let {
                            it.copy(
                                review = it.review.copy(
                                    reactions = saved
                                )
                            )
                        })
                    }
                },
                {
                    setState { copy(details = old) }; toast(
                    it.message ?: strings.get(R.string.reviews_vote_error)
                )
                },
            )
        }
    }

    private fun delete() {
        if (!currentState.isOwner) return; viewModelScope.launch {
            setState { copy(deleting = true) }; runCatching {
            deleteReview(
                reviewId
            )
        }.fold({
            if (it) {
                setEffect(ReviewDetailsState.Effect.Deleted); nav.back()
            } else toast(strings.get(R.string.reviews_delete_error))
        }, { toast(it.message ?: strings.get(R.string.reviews_delete_error)) }); setState {
            copy(
                deleting = false
            )
        }
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

    private fun toast(message: String) = setEffect(ReviewDetailsState.Effect.ShowToast(message))
}
