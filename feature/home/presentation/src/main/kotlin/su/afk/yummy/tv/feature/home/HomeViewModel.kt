package su.afk.yummy.tv.feature.home

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.home.model.HomeFeed
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.domain.home.usecase.GetHomeFeedUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.home.utils.toPlayerSkips
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.getPlayerDest
import su.afk.yummy.tv.feature.player.isTrustedPlaceholderMigrationTarget
import su.afk.yummy.tv.feature.player.resolveContinueWatchingTarget
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val collectionNavigator: ICollectionNavigator,
    private val playerNavigator: IPlayerNavigator,
    private val getHomeFeed: GetHomeFeedUseCase,
    private val getAnimePreview: GetAnimePreviewUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val watchProgressStore: WatchProgressStore,
    private val stringProvider: StringProvider,
) : BaseViewModelNew<HomeState.State, HomeState.Event, HomeState.Effect>(savedStateHandle) {

    override fun createInitialState() = HomeState.State()

    private var previewJob: Job? = null
    private val previewCache = mutableMapOf<Int, AnimePreview>()
    private val prefetchingPreviews = mutableSetOf<Int>()
    private val previewPrefetchSemaphore = Semaphore(2)

    init {
        load()
        watchProgressStore.observeContinueWatching()
            .map { entries ->
                WatchProgressStore.latestByAnime(entries)
            }
            .flowOn(Dispatchers.Default)
            .onEach { inProgress ->
                setState {
                    copy(
                        continueWatching = inProgress,
                        isContinueWatchingLoaded = true,
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: HomeState.Event) {
        when (event) {
            is HomeState.Event.AnimeSelected -> {
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            }

            is HomeState.Event.CollectionSelected -> {
                setSelectedItemRestoreState(event.sourceSectionId, event.displayId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }
            is HomeState.Event.VideoSelected -> Unit
            is HomeState.Event.ContinueWatchingSelected -> {
                setState {
                    copy(
                        continueWatchingRestoreToken = continueWatchingRestoreToken + 1,
                        focusedItemId = null,
                        focusedSectionId = null,
                        focusedPreview = null,
                    )
                }
                launchContinueWatching(event.entry)
            }
            HomeState.Event.RetrySelected -> load()
            is HomeState.Event.ItemFocused -> onItemFocused(
                event.sectionId,
                event.displayId,
                event.animeId
            )
            is HomeState.Event.HeroItemVisible -> prefetchHeroPreviewWindow(event.displayId)
            HomeState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }
        }
    }

    private fun onItemFocused(sectionId: String, displayId: Int, animeId: Int?) {
        if (currentState.focusedSectionId == sectionId && currentState.focusedItemId == displayId) return
        previewJob?.cancel()
        prefetchHeroPreviewWindow(displayId)
        if (animeId == null) {
            setState {
                copy(
                    focusedSectionId = sectionId,
                    focusedItemId = displayId,
                    focusedPreview = null,
                )
            }
            return
        }
        val cachedPreview = previewCache[animeId]
        setState {
            copy(
                focusedSectionId = sectionId,
                focusedItemId = displayId,
                focusedPreview = cachedPreview,
                animePreviews = previewCache.toMap(),
            )
        }
        if (cachedPreview != null) return
        previewJob = viewModelScope.launch {
            delay(PREVIEW_FOCUS_DEBOUNCE_MS)
            runCatching { getAnimePreview(animeId) }.onSuccess { preview ->
                previewCache[animeId] = preview
                val previews = previewCache.toMap()
                if (
                    currentState.focusedItemId == displayId &&
                    currentState.focusedSectionId == sectionId
                ) {
                    setState { copy(focusedPreview = preview, animePreviews = previews) }
                } else {
                    setState { copy(animePreviews = previews) }
                }
            }
        }
    }

    private fun setSelectedItemRestoreState(sourceSectionId: String?, displayId: Int?) {
        setState {
            copy(
                focusedSectionId = sourceSectionId ?: focusedSectionId,
                focusedItemId = displayId ?: focusedItemId,
                restoreFocusedItemOnEnter = true,
            )
        }
    }

    private fun prefetchHeroPreviewWindow(displayId: Int) {
        val heroItems = currentState.feed?.heroItems.orEmpty()
        val currentIndex = heroItems.indexOfFirst { it.id == displayId }
        if (currentIndex == -1) return
        heroItems.getOrNull(currentIndex)?.animeId?.let(::prefetchPreview)
        heroItems.getOrNull(currentIndex + 1)?.animeId?.let(::prefetchPreview)
    }

    private fun prefetchPreview(animeId: Int) {
        if (previewCache.containsKey(animeId) || !prefetchingPreviews.add(animeId)) return
        viewModelScope.launch {
            try {
                previewPrefetchSemaphore.withPermit {
                    if (previewCache.containsKey(animeId)) return@withPermit
                    runCatching { getAnimePreview(animeId) }.onSuccess { preview ->
                        previewCache[animeId] = preview
                        setState { copy(animePreviews = previewCache.toMap()) }
                    }
                }
            } finally {
                prefetchingPreviews.remove(animeId)
            }
        }
    }

    private fun launchContinueWatching(entry: WatchProgressEntry) {
        viewModelScope.launch {
            val videos = if (entry.animeId != 0)
                runCatching { getAnimeVideos(entry.animeId) }.getOrNull().orEmpty()
            else emptyList()

            val videoSources = videos.map { it.toPlayerVideoSource() }
            val progressVideo = entry.toPlayerVideoSource()
            val target = resolveContinueWatchingTarget(progressVideo, videoSources)

            migratePlaceholderEpisode(entry, progressVideo, target.video)

            nav.navigate(playerNavigator.getPlayerDest(
                video = target.video,
                allVideos = target.allVideos,
                animeTitle = entry.animeTitle,
                animeId = entry.animeId,
                posterUrl = entry.posterUrl,
            ))
        }
    }

    private suspend fun migratePlaceholderEpisode(
        entry: WatchProgressEntry,
        progressVideo: PlayerVideoSource,
        targetVideo: PlayerVideoSource,
    ) {
        if (!progressVideo.isTrustedPlaceholderMigrationTarget(targetVideo)) return
        watchProgressStore.save(
            animeId = entry.animeId,
            episode = targetVideo.episode,
            videoId = targetVideo.id,
            episodeUrl = targetVideo.iframeUrl,
            positionMs = entry.positionMs,
            durationMs = entry.durationMs,
            animeTitle = entry.animeTitle,
            posterUrl = entry.posterUrl,
            playerName = targetVideo.player,
            dubbing = targetVideo.dubbing,
            screenshotUrl = entry.screenshotUrl,
        )
        watchProgressStore.delete(entry.animeId, entry.episode)
    }

    private val HomeFeedItem.animeId: Int?
        get() = (action as? HomeFeedItemAction.OpenSeries)?.seriesId

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getHomeFeed() }.fold(
                onSuccess = { feed ->
                    setState { copy(isLoading = false, feed = feed) }
                    prefetchInitialHeroPreviews(feed)
                },
                onFailure = { e ->
                    setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.home_load_error)) }
                },
            )
        }
    }

    private fun prefetchInitialHeroPreviews(feed: HomeFeed) {
        feed.heroItems
            .take(2)
            .mapNotNull { it.animeId }
            .forEach(::prefetchPreview)
    }

    private companion object {
        const val PREVIEW_FOCUS_DEBOUNCE_MS = 250L
    }
}

private fun AnimeVideo.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = id,
    episode = episode,
    dubbing = dubbing,
    player = player,
    iframeUrl = iframeUrl,
    views = views,
    skips = skips.toPlayerSkips(),
)

private fun WatchProgressEntry.toPlayerVideoSource(): PlayerVideoSource = PlayerVideoSource(
    id = videoId,
    episode = episode,
    dubbing = dubbing,
    player = playerName,
    iframeUrl = episodeUrl,
)
