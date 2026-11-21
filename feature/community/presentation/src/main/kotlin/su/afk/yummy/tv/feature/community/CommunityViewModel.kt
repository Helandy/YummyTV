package su.afk.yummy.tv.feature.community

import androidx.navigation3.runtime.NavKey
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.bloggers.usecase.GetBloggerVideosUseCase
import su.afk.yummy.tv.domain.posts.model.PostSort
import su.afk.yummy.tv.domain.posts.usecase.GetPostsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.bloggers.IBloggerVideosNavigator
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.community.model.CommunitySection
import su.afk.yummy.tv.feature.community.presentation.R
import su.afk.yummy.tv.feature.glossary.IGlossaryNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import su.afk.yummy.tv.feature.pages.ISitePagesNavigator
import su.afk.yummy.tv.feature.posts.IPostsNavigator
import su.afk.yummy.tv.feature.reviews.IReviewsNavigator
import su.afk.yummy.tv.feature.schedule.IScheduleNavigator
import javax.inject.Inject

/** Сколько элементов каждой ленты показываем на хабе; полный список открывается по «Все». */
private const val COMMUNITY_FEED_LIMIT = 10

@HiltViewModel
class CommunityViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val postsNavigator: IPostsNavigator,
    private val bloggerVideosNavigator: IBloggerVideosNavigator,
    private val messagesNavigator: IMessagesNavigator,
    private val sitePagesNavigator: ISitePagesNavigator,
    private val glossaryNavigator: IGlossaryNavigator,
    private val accountNavigator: IAccountNavigator,
    private val reviewsNavigator: IReviewsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val scheduleNavigator: IScheduleNavigator,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val getPosts: GetPostsUseCase,
    private val getBloggerVideos: GetBloggerVideosUseCase,
) : BaseViewModel<CommunityState.State, CommunityState.Event, CommunityState.Effect>() {

    override fun createInitialState() = CommunityState.State()

    /** Id текущего пользователя; 0 — гость. Нужен только для перехода к списку друзей. */
    private var userId = 0

    init {
        observeAccountSession().onEach { userId = it.userId }.launchIn(viewModelScope)
        loadNews()
        loadVideos()
    }

    override fun onEvent(event: CommunityState.Event) {
        when (event) {
            is CommunityState.Event.SectionSelected -> openSection(event.section)
            CommunityState.Event.AllNewsSelected -> nav.navigate(postsNavigator.list())
            CommunityState.Event.AllVideosSelected -> nav.navigate(bloggerVideosNavigator.feed())
            is CommunityState.Event.NewsSelected -> nav.navigate(postsNavigator.details(event.postId))
            is CommunityState.Event.VideoSelected -> nav.navigate(bloggerVideosNavigator.video(event.videoId))
            CommunityState.Event.NewsRetrySelected -> loadNews()
            CommunityState.Event.VideosRetrySelected -> loadVideos()
            CommunityState.Event.RefreshSelected -> refresh()
        }
    }

    private fun openSection(section: CommunitySection) {
        if (section == CommunitySection.FRIENDS) {
            // Гостю список друзей недоступен: ведём на вкладку профиля с формой входа.
            if (userId > 0) {
                nav.navigate(accountNavigator.getUserFriendsDest(userId))
            } else {
                nav.switchRoot(RootTab.ACCOUNT)
            }
            return
        }
        nav.navigate(destinationOf(section))
    }

    private fun destinationOf(section: CommunitySection): NavKey = when (section) {
        CommunitySection.REVIEWS -> reviewsNavigator.feed()
        CommunitySection.COLLECTIONS -> collectionNavigator.getCollectionsCatalogDest()
        CommunitySection.SCHEDULE -> scheduleNavigator.getScheduleDest()
        CommunitySection.CHAT -> messagesNavigator.dialogs()
        CommunitySection.FRIENDS -> error("Friends are opened by openSection")
        CommunitySection.USERS -> accountNavigator.getUserSearchDest()
        CommunitySection.ABOUT -> sitePagesNavigator.pages()
        CommunitySection.GLOSSARY -> glossaryNavigator.getGlossaryDest()
    }

    /** Обновляет обе ленты; уже показанные карточки остаются на месте, пока идёт запрос. */
    private fun refresh() {
        if (currentState.isRefreshing) return
        viewModelScope.launch {
            setState { copy(isRefreshing = true) }
            joinAll(loadNews(), loadVideos())
            setState { copy(isRefreshing = false) }
        }
    }

    private fun loadNews(): Job = viewModelScope.launch {
        setState { copy(isNewsLoading = news.isEmpty(), newsError = null) }
        runSuspendCatching { getPosts(null, PostSort.NEW.apiValue, COMMUNITY_FEED_LIMIT, 0) }.fold(
            onSuccess = { posts ->
                setState { copy(news = posts.toImmutableList(), isNewsLoading = false) }
            },
            onFailure = { error ->
                setState {
                    copy(
                        isNewsLoading = false,
                        // Старую ленту при неудачном обновлении не прячем.
                        newsError = if (news.isEmpty()) {
                            UiText.Failure(error, UiText.Res(R.string.community_news_load_error))
                        } else {
                            null
                        },
                    )
                }
            },
        )
    }

    private fun loadVideos(): Job = viewModelScope.launch {
        setState { copy(isVideosLoading = videos.isEmpty(), videosError = null) }
        runSuspendCatching { getBloggerVideos(limit = COMMUNITY_FEED_LIMIT) }.fold(
            onSuccess = { videos ->
                setState { copy(videos = videos.toImmutableList(), isVideosLoading = false) }
            },
            onFailure = { error ->
                setState {
                    copy(
                        isVideosLoading = false,
                        videosError = if (videos.isEmpty()) {
                            UiText.Failure(error, UiText.Res(R.string.community_videos_load_error))
                        } else {
                            null
                        },
                    )
                }
            },
        )
    }
}
