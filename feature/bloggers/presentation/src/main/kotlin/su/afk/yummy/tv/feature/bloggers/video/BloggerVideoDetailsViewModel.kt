package su.afk.yummy.tv.feature.bloggers.video

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoReaction
import su.afk.yummy.tv.domain.bloggers.model.BloggerVideoVote
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideoDetailsUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.SetBloggerVideoVoteUseCase
import su.afk.yummy.tv.core.model.comments.CommentTargetType
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.bloggers.presentation.R
import su.afk.yummy.tv.feature.comments.ICommentsNavigator

@HiltViewModel(assistedFactory = BloggerVideoDetailsViewModel.Factory::class)
class BloggerVideoDetailsViewModel @AssistedInject constructor(
    @Assisted private val videoId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val navigator: IBloggerVideosNavigator,
    private val commentsNavigator: ICommentsNavigator,
    private val getDetails: GetBloggerVideoDetailsUseCase,
    private val setVote: SetBloggerVideoVoteUseCase,
    private val strings: StringProvider,
    settingsStore: YaniAccountSettingsStore,
) : BaseViewModel<BloggerVideoDetailsState.State, BloggerVideoDetailsState.Event, BloggerVideoDetailsState.Effect>() {
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
                        error = errorHandler.parse(error, navigate = false).message,
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
                                errorHandler.parse(error, navigate = false).message
                            )
                        )
                    }
                },
            )
        }
        voteJob = null
        setState { copy(voting = false) }
    }

    @AssistedFactory
    interface Factory {
        fun create(videoId: Int): BloggerVideoDetailsViewModel
    }
}
