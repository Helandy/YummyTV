package su.afk.yummy.tv.feature.posts.details

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.domain.posts.model.PostVote
import su.afk.yummy.tv.domain.posts.usecase.GetPostDetailsUseCase
import su.afk.yummy.tv.domain.posts.usecase.RemovePostVoteUseCase
import su.afk.yummy.tv.domain.posts.usecase.VotePostUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.comments.ICommentsNavigator
import su.afk.yummy.tv.feature.commonscreen.navigator.IImageViewNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.posts.presentation.R
import su.afk.yummy.tv.feature.posts.utils.imageUrls

@HiltViewModel(assistedFactory = PostDetailsViewModel.Factory::class)
class PostDetailsViewModel @AssistedInject constructor(
    @Assisted private val postId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val getPostDetails: GetPostDetailsUseCase,
    private val votePost: VotePostUseCase,
    private val removePostVote: RemovePostVoteUseCase,
    private val accountNavigator: IAccountNavigator,
    private val detailsNavigator: IDetailsNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val imageViewNavigator: IImageViewNavigator,
    private val strings: StringProvider,
    settingsStore: YaniAccountSettingsStore,
) : BaseViewModel<PostDetailsState.State, PostDetailsState.Event, PostDetailsState.Effect>() {
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

    /** Галерея из всех картинок поста в порядке показа, чтобы листать их, не выходя из просмотра. */
    private fun openImage(url: String) {
        val images = currentState.details?.imageUrls().orEmpty().ifEmpty { listOf(url) }
        nav.navigate(
            imageViewNavigator(
                imageUrl = url,
                imageUrls = images,
                selectedIndex = images.indexOf(url).coerceAtLeast(0),
            ),
        )
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

            is PostDetailsState.Event.ImageSelected -> openImage(event.url)
        }
    }

    private fun load() = viewModelScope.launch {
        setState { copy(loading = true, error = null) }
        runSuspendCatching { getPostDetails(postId) }.fold(
            { setState { copy(loading = false, details = it) } },
            {
                setState {
                    copy(
                        loading = false,
                        error = errorHandler.parse(it).message
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
            runSuspendCatching {
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
                    setState { copy(details = old, voting = false) }
                    toast(strings.get(R.string.posts_vote_error))
                },
            )
        }
    }

    private fun toast(message: String) = setEffect(PostDetailsState.Effect.ShowToast(message))
}
