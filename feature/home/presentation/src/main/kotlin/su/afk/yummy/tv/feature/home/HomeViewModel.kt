package su.afk.yummy.tv.feature.home

import androidx.lifecycle.SavedStateHandle
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
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.AnimePreview
import su.afk.yummy.tv.domain.anime.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.anime.GetAnimeVideosUseCase
import su.afk.yummy.tv.domain.home.GetHomeFeedUseCase
import su.afk.yummy.tv.domain.home.HomeFeedItem
import su.afk.yummy.tv.domain.home.HomeFeedItemAction
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.presentation.R
import su.afk.yummy.tv.feature.player.IPlayerNavigator
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

    init {
        load()
        watchProgressStore.observeAll()
            .onEach { entries ->
                val inProgress = entries
                    .filter { e ->
                        e.durationMs > 0 && e.positionMs >= 30_000 &&
                        e.positionMs.toFloat() / e.durationMs < 0.90f
                    }
                    .sortedByDescending { it.updatedAt }
                    .distinctBy { it.animeId to it.episode }
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
            is HomeState.Event.AnimeSelected -> nav.navigate(detailsNavigator.getDetailsDest(event.seriesId))
            is HomeState.Event.CollectionSelected -> nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            is HomeState.Event.VideoSelected -> Unit
            is HomeState.Event.ContinueWatchingSelected -> launchContinueWatching(event.entry)
            HomeState.Event.RetrySelected -> load()
            is HomeState.Event.ItemFocused -> onItemFocused(event.displayId, event.animeId)
        }
    }

    private fun onItemFocused(displayId: Int, animeId: Int?) {
        if (currentState.focusedItemId == displayId) return
        previewJob?.cancel()
        prefetchAdjacentHeroPreviews(displayId)
        if (animeId == null) {
            setState { copy(focusedItemId = displayId, focusedPreview = null) }
            return
        }
        val cachedPreview = previewCache[animeId]
        setState { copy(focusedItemId = displayId, focusedPreview = cachedPreview, animePreviews = previewCache.toMap()) }
        if (cachedPreview != null) return
        previewJob = viewModelScope.launch {
            runCatching { getAnimePreview(animeId) }.onSuccess { preview ->
                previewCache[animeId] = preview
                val previews = previewCache.toMap()
                if (currentState.focusedItemId == displayId) {
                    setState { copy(focusedPreview = preview, animePreviews = previews) }
                } else {
                    setState { copy(animePreviews = previews) }
                }
            }
        }
    }

    private fun prefetchAdjacentHeroPreviews(displayId: Int) {
        val heroItems = currentState.feed?.heroItems.orEmpty()
        val currentIndex = heroItems.indexOfFirst { it.id == displayId }
        if (currentIndex == -1) return
        listOf(currentIndex - 1, currentIndex + 1)
            .mapNotNull { index -> heroItems.getOrNull(index)?.animeId }
            .forEach { animeId -> prefetchPreview(animeId) }
    }

    private fun prefetchPreview(animeId: Int) {
        if (previewCache.containsKey(animeId) || !prefetchingPreviews.add(animeId)) return
        viewModelScope.launch {
            runCatching { getAnimePreview(animeId) }.onSuccess { preview ->
                previewCache[animeId] = preview
                setState { copy(animePreviews = previewCache.toMap()) }
            }
            prefetchingPreviews.remove(animeId)
        }
    }

    private fun launchContinueWatching(entry: su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry) {
        viewModelScope.launch {
            val videos = if (entry.animeId != 0)
                runCatching { getAnimeVideos(entry.animeId) }.getOrNull().orEmpty()
            else emptyList()

            val kodikVideos = videos.filter { it.iframeUrl.contains("kodik", ignoreCase = true) }
            val targetVideo = kodikVideos.firstOrNull { it.episode == entry.episode }
            val iframeUrl = targetVideo?.iframeUrl ?: entry.episodeUrl
            val resolvedDubbing = targetVideo?.dubbing ?: entry.dubbing
            val resolvedPlayer = targetVideo?.player ?: entry.playerName

            val episodeGroup = kodikVideos
                .filter { it.dubbing == resolvedDubbing }
                .sortedBy { it.episode.toIntOrNull() ?: Int.MAX_VALUE }

            val urls = episodeGroup.map { it.iframeUrl }.ifEmpty { listOf(iframeUrl) }
            val numbers = episodeGroup.map { it.episode }.ifEmpty { listOf(entry.episode) }
            val idx = urls.indexOf(iframeUrl).coerceAtLeast(0)

            nav.navigate(playerNavigator.getPlayerDest(
                iframeUrl = iframeUrl,
                animeTitle = entry.animeTitle,
                episode = entry.episode,
                playerName = resolvedPlayer,
                dubbing = resolvedDubbing,
                episodeUrls = urls,
                episodeNumbers = numbers,
                currentEpisodeIndex = idx,
                animeId = entry.animeId,
                posterUrl = entry.posterUrl,
            ))
        }
    }

    private val HomeFeedItem.animeId: Int?
        get() = (action as? HomeFeedItemAction.OpenSeries)?.seriesId

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getHomeFeed() }.fold(
                onSuccess = { feed -> setState { copy(isLoading = false, feed = feed) } },
                onFailure = { e ->
                    setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.home_load_error)) }
                },
            )
        }
    }
}
