package su.afk.yummy.tv.feature.posts.details

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
import su.afk.yummy.tv.domain.posts.model.PostReaction
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.domain.posts.usecase.GetPostDetailsUseCase
import su.afk.yummy.tv.domain.posts.usecase.RemovePostVoteUseCase
import su.afk.yummy.tv.domain.posts.usecase.VotePostUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.posts.presentation.R

@HiltViewModel(assistedFactory = PostDetailsViewModel.Factory::class)
class PostDetailsViewModel @AssistedInject constructor(
    @Assisted private val postId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getPostDetails: GetPostDetailsUseCase,
    private val votePost: VotePostUseCase,
    private val removePostVote: RemovePostVoteUseCase,
    private val accountNavigator: IAccountNavigator,
    private val detailsNavigator: IDetailsNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val strings: StringProvider,
    settingsStore: SettingsStore,
) : BaseViewModelNew<PostDetailsState.State, PostDetailsState.Event, PostDetailsState.Effect>(
    savedStateHandle
) {
    @AssistedFactory
    interface Factory {
        fun create(postId: Int): PostDetailsViewModel
    }

    override fun createInitialState() = PostDetailsState.State()

    init {
        settingsStore.yaniUserId.onEach { setState { copy(currentUserId = it) } }
            .launchIn(viewModelScope)
        load()
    }

    override fun onEvent(event: PostDetailsState.Event) {
        when (event) {
            PostDetailsState.Event.BackSelected -> nav.back()
            PostDetailsState.Event.RetrySelected -> load()
            is PostDetailsState.Event.VoteSelected -> vote(event.vote)
            is PostDetailsState.Event.AnimeSelected -> nav.navigate(
                detailsNavigator.getDetailsDest(
                    event.animeId
                )
            )

            is PostDetailsState.Event.AuthorSelected -> nav.navigate(
                accountNavigator.getUserProfileDest(
                    event.userId
                )
            )

            PostDetailsState.Event.CommentsSelected -> nav.navigate(
                commentsNavigator.getCommentsDest(CommentTargetType.POST, postId)
            )
        }
    }

    private fun load() = viewModelScope.launch {
        setState { copy(loading = true, error = null) }
        runCatching { getPostDetails(postId) }.fold(
            { setState { copy(loading = false, details = it) } },
            {
                setState {
                    copy(
                        loading = false,
                        error = it.message ?: strings.get(R.string.posts_load_error)
                    )
                }
            },
        )
    }

    private fun vote(target: PostVote) {
        if (currentState.currentUserId <= 0) {
            toast(strings.get(R.string.posts_auth_required)); return
        }
        if (currentState.voting) return
        val old = currentState.details ?: return
        val actualTarget = if (old.reaction.vote == target) PostVote.NONE else target
        setState {
            copy(
                details = old.copy(reaction = old.reaction.optimistic(actualTarget)),
                voting = true
            )
        }
        viewModelScope.launch {
            runCatching {
                if (actualTarget == PostVote.NONE) removePostVote(postId) else votePost(
                    postId,
                    actualTarget
                )
            }.fold(
                { saved ->
                    setState {
                        copy(
                            details = details?.copy(reaction = saved),
                            voting = false
                        )
                    }
                },
                {
                    setState { copy(details = old, voting = false) }; toast(
                    it.message ?: strings.get(
                        R.string.posts_vote_error
                    )
                )
                },
            )
        }
    }

    private fun PostReaction.optimistic(target: PostVote): PostReaction {
        var nextLikes = likes - if (vote == PostVote.LIKE) 1 else 0
        var nextDislikes = dislikes - if (vote == PostVote.DISLIKE) 1 else 0
        if (target == PostVote.LIKE) nextLikes++
        if (target == PostVote.DISLIKE) nextDislikes++
        return copy(nextLikes.coerceAtLeast(0), nextDislikes.coerceAtLeast(0), target)
    }

    private fun toast(message: String) = setEffect(PostDetailsState.Effect.ShowToast(message))
}
