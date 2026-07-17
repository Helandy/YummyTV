package su.afk.yummy.tv.feature.bloggers.video

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.runSuspendCatching
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideoDetailsUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.SetBloggerVideoVoteUseCase
import su.afk.yummy.tv.domain.comments.model.CommentTargetType
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.bloggers.presentation.R
import su.afk.yummy.tv.feature.comments.ICommentsNavigator

@HiltViewModel(assistedFactory = BloggerVideoDetailsViewModel.Factory::class)
class BloggerVideoDetailsViewModel @AssistedInject constructor(
    @Assisted private val videoId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val navigator: IBloggerVideosNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val getDetails: GetBloggerVideoDetailsUseCase,
    private val setVote: SetBloggerVideoVoteUseCase,
    private val strings: StringProvider,
    settingsStore: SettingsStore,
) : BaseViewModelNew<BloggerVideoDetailsState.State, BloggerVideoDetailsState.Event, BloggerVideoDetailsState.Effect>(
    savedStateHandle
) {
    override fun createInitialState() = BloggerVideoDetailsState.State()

    private var confirmedReaction: BloggerVideoReaction? = null
    private var queuedVote: BloggerVideoVote? = null
    private var voteJob: Job? = null

    init {
        settingsStore.yaniUserId.onEach { setState { copy(currentUserId = it) } }
            .launchIn(viewModelScope)
        load()
    }

    override fun onEvent(event: BloggerVideoDetailsState.Event) {
        when (event) {
            BloggerVideoDetailsState.Event.BackSelected -> nav.back()
            BloggerVideoDetailsState.Event.RetrySelected -> load()
            BloggerVideoDetailsState.Event.WatchSelected -> currentState.video?.let {
                setEffect(
                    BloggerVideoDetailsState.Effect.OpenVideo(it.watchUrl)
                )
            }

            BloggerVideoDetailsState.Event.BloggerSelected -> currentState.video?.let {
                nav.navigate(
                    navigator.blogger(it.creator.id)
                )
            }

            is BloggerVideoDetailsState.Event.VoteSelected -> vote(event.vote)
            BloggerVideoDetailsState.Event.CommentsSelected -> nav.navigate(
                commentsNavigator.getCommentsDest(CommentTargetType.BLOG_VIDEO, videoId)
            )
        }
    }

    private fun load() = viewModelScope.launch {
        setState { copy(loading = true, error = null) }
        runSuspendCatching { getDetails(videoId) }.fold(
            { video ->
                confirmedReaction = video.reaction
                setState { copy(video = video, loading = false) }
            },
            { error ->
                setState {
                    copy(
                        loading = false,
                        error = error.message ?: strings.get(R.string.blogger_video_load_error)
                    )
                }
            },
        )
    }

    private fun vote(selected: BloggerVideoVote) {
        if (currentState.currentUserId <= 0) {
            setEffect(BloggerVideoDetailsState.Effect.ShowToast(strings.get(R.string.bloggers_auth_required)))
            return
        }
        val old = currentState.video ?: return
        val target = if (old.reaction.vote == selected) BloggerVideoVote.NONE else selected
        queuedVote = target
        setState {
            copy(
                video = old.copy(reaction = old.reaction.optimistic(target)),
                voting = true
            )
        }
        if (voteJob?.isActive == true) return
        voteJob = viewModelScope.launch { drainVoteQueue() }
    }

    private suspend fun drainVoteQueue() {
        while (true) {
            val target = queuedVote ?: break
            queuedVote = null
            runSuspendCatching { setVote(videoId, target) }.fold(
                { reaction ->
                    confirmedReaction = reaction
                    val nextTarget = queuedVote
                    setState {
                        copy(
                            video = video?.copy(
                                reaction = nextTarget?.let { reaction.optimistic(it) } ?: reaction
                            ),
                            voting = nextTarget != null,
                        )
                    }
                },
                { error ->
                    val nextTarget = queuedVote
                    val rollback = confirmedReaction
                    setState {
                        copy(
                            video = video?.let { currentVideo ->
                                rollback?.let { confirmed ->
                                    currentVideo.copy(
                                        reaction = nextTarget?.let { confirmed.optimistic(it) }
                                            ?: confirmed
                                    )
                                } ?: currentVideo
                            },
                            voting = nextTarget != null,
                        )
                    }
                    if (nextTarget == null) {
                        setEffect(
                            BloggerVideoDetailsState.Effect.ShowToast(
                                error.message ?: strings.get(R.string.blogger_vote_error)
                            )
                        )
                    }
                },
            )
        }
        voteJob = null
        setState { copy(voting = false) }
    }

    private fun BloggerVideoReaction.optimistic(target: BloggerVideoVote): BloggerVideoReaction {
        var nextLikes = likes - if (vote == BloggerVideoVote.LIKE) 1 else 0
        var nextDislikes = dislikes - if (vote == BloggerVideoVote.DISLIKE) 1 else 0
        if (target == BloggerVideoVote.LIKE) nextLikes++
        if (target == BloggerVideoVote.DISLIKE) nextDislikes++
        return copy(
            likes = nextLikes.coerceAtLeast(0),
            dislikes = nextDislikes.coerceAtLeast(0),
            vote = target
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(videoId: Int): BloggerVideoDetailsViewModel
    }
}
