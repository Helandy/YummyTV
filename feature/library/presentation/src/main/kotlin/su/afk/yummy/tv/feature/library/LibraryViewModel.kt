package su.afk.yummy.tv.feature.library

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.domain.account.usecase.GetUserAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.GetUserFavoriteAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveAnimeListUseCase
import su.afk.yummy.tv.domain.account.usecase.RemoveWatchedVideoUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeFavoriteUseCase
import su.afk.yummy.tv.domain.account.usecase.SetAnimeListUseCase
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.utils.LocalLibrarySyncResult
import su.afk.yummy.tv.feature.library.utils.toPlayerSkips
import su.afk.yummy.tv.feature.library.utils.userAnimeList
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.getPlayerDest
import su.afk.yummy.tv.feature.player.isPlaceholderEpisode
import su.afk.yummy.tv.feature.player.selectContinueWatchingVideo
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val libraryStore: LibraryStore,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val getUserAnimeList: GetUserAnimeListUseCase,
    private val getUserFavoriteAnimeList: GetUserFavoriteAnimeListUseCase,
    private val setAnimeList: SetAnimeListUseCase,
    private val removeAnimeList: RemoveAnimeListUseCase,
    private val setAnimeFavorite: SetAnimeFavoriteUseCase,
    private val removeWatchedVideo: RemoveWatchedVideoUseCase,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val playerNavigator: IPlayerNavigator,
    private val getAnimePreview: GetAnimePreviewUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
) : BaseViewModelNew<LibraryState.State, LibraryState.Event, LibraryState.Effect>(savedStateHandle) {

    override fun createInitialState() = LibraryState.State()

    private var previewJob: Job? = null
    private var signedInUserId: Int = 0

    init {
        libraryStore.observeAll()
            .onEach { entries -> setState { copy(items = entries) } }
            .launchIn(viewModelScope)

        watchProgressStore.observeContinueWatching()
            .map { entries ->
                entries
                    .groupBy { it.animeId }
                    .values
                    .map { group -> group.maxBy { it.updatedAt } }
                    .sortedByDescending { it.updatedAt }
            }
            .flowOn(Dispatchers.Default)
            .onEach { entries -> setState { copy(continueWatching = entries) } }
            .launchIn(viewModelScope)
        settingsStore.yaniUserId
            .onEach { userId ->
                signedInUserId = userId
                setState { copy(isSignedIn = userId > 0) }
                if (userId > 0) loadRemoteLists(userId)
            }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: LibraryState.Event) {
        when (event) {
            is LibraryState.Event.AnimeSelected ->
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            is LibraryState.Event.RemoteAnimeSelected ->
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            is LibraryState.Event.ContinueWatchingSelected ->
                launchContinueWatching(event.entry)
            is LibraryState.Event.ItemFocused ->
                onItemFocused(event.animeId)
            is LibraryState.Event.TabSelected ->
                setState { copy(selectedTab = event.tab, focusedItemId = null, focusedPreview = null) }
            LibraryState.Event.ScreenResumed -> refreshRemoteLists()
            is LibraryState.Event.RemoveLibraryEntry ->
                viewModelScope.launch {
                    libraryStore.remove(event.animeId)
                    setEffect(LibraryState.Effect.ItemRemoved)
                }
            is LibraryState.Event.RemoveFavoriteEntry ->
                viewModelScope.launch {
                    libraryStore.setFavorite(event.animeId, title = "", poster = null, favorite = false)
                    setEffect(LibraryState.Effect.ItemRemoved)
                }
            is LibraryState.Event.RemoveWatchProgress ->
                viewModelScope.launch {
                    val entries = currentState.continueWatching.filter { it.animeId == event.animeId }
                    watchProgressStore.deleteByAnimeId(event.animeId)
                    entries.mapNotNull { it.videoId.takeIf { id -> id > 0 } }.distinct().forEach { videoId ->
                        runCatching { removeWatchedVideo(videoId) }
                    }
                    setEffect(LibraryState.Effect.ItemRemoved)
                }
            is LibraryState.Event.RemoveRemoteEntry -> removeRemoteEntry(event)
        }
    }

    private fun refreshRemoteLists() {
        if (signedInUserId > 0) loadRemoteLists(signedInUserId)
    }

    private fun loadRemoteLists(userId: Int) {
        viewModelScope.launch {
            runCatching {
                val remote = fetchRemoteLists(userId)
                val syncResult = syncLocalMissingToRemote(remote)
                val currentRemote = if (syncResult.syncedAny) fetchRemoteLists(userId) else remote
                currentRemote to syncResult.error
            }.fold(
                onSuccess = { (remote, syncError) -> setState { copy(remoteItems = remote, remoteError = syncError) } },
                onFailure = { setState { copy(remoteError = it.message) } },
            )
        }
    }

    private suspend fun fetchRemoteLists(userId: Int): Map<LibraryTab, List<UserAnimeListItem>> = coroutineScope {
        val watching = async { getUserAnimeList(userId, UserAnimeList.WATCHING) }
        val favorites = async { getUserFavoriteAnimeList(userId) }
        val planned = async { getUserAnimeList(userId, UserAnimeList.PLANNED) }
        val completed = async { getUserAnimeList(userId, UserAnimeList.COMPLETED) }
        val postponed = async { getUserAnimeList(userId, UserAnimeList.POSTPONED) }
        val dropped = async { getUserAnimeList(userId, UserAnimeList.DROPPED) }

        mapOf(
            LibraryTab.WATCHING to watching.await(),
            LibraryTab.FAVORITES to favorites.await(),
            LibraryTab.PLANNED to planned.await(),
            LibraryTab.COMPLETED to completed.await(),
            LibraryTab.POSTPONED to postponed.await(),
            LibraryTab.DROPPED to dropped.await(),
        )
    }

    private suspend fun syncLocalMissingToRemote(
        remote: Map<LibraryTab, List<UserAnimeListItem>>,
    ): LocalLibrarySyncResult {
        val remotePrimaryAnimeIds = remote
            .filterKeys { it != LibraryTab.FAVORITES }
            .values
            .flatten()
            .map { it.animeId }
            .toSet()
        val remoteFavoriteAnimeIds = remote[LibraryTab.FAVORITES].orEmpty().map { it.animeId }.toSet()
        val localItems = libraryStore.getAll()
        val localMissing = localItems
            .filter { it.listId >= 0 }
            .filterNot { it.animeId in remotePrimaryAnimeIds }
        var syncedAny = false
        var firstError: String? = null

        localMissing.forEach { entry ->
            val list = entry.userAnimeList() ?: return@forEach
            runCatching {
                setAnimeList(entry.animeId, list)
            }.fold(
                onSuccess = { syncedAny = true },
                onFailure = { if (firstError == null) firstError = it.message },
            )
        }

        localItems
            .filter { it.isFavorite }
            .filterNot { it.animeId in remoteFavoriteAnimeIds }
            .forEach { entry ->
                runCatching {
                    setAnimeFavorite(entry.animeId, true)
                }.fold(
                    onSuccess = { syncedAny = true },
                    onFailure = { if (firstError == null) firstError = it.message },
                )
            }

        return LocalLibrarySyncResult(syncedAny = syncedAny, error = firstError)
    }

    private fun removeRemoteEntry(event: LibraryState.Event.RemoveRemoteEntry) {
        val previous = currentState.remoteItems
        val tab = currentState.selectedTab
        setState {
            copy(remoteItems = remoteItems + (tab to remoteItems[tab].orEmpty().filterNot { it.animeId == event.animeId }))
        }
        viewModelScope.launch {
            val result = runCatching {
                if (event.favorite) {
                    setAnimeFavorite(event.animeId, false)
                    libraryStore.setFavorite(event.animeId, title = "", poster = null, favorite = false)
                } else {
                    removeAnimeList(event.animeId)
                    libraryStore.remove(event.animeId)
                }
            }
            if (result.isFailure) {
                setState { copy(remoteItems = previous, remoteError = result.exceptionOrNull()?.message) }
            } else {
                setEffect(LibraryState.Effect.ItemRemoved)
            }
        }
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        previewJob?.cancel()
        setState { copy(focusedItemId = animeId, focusedPreview = null) }
        previewJob = viewModelScope.launch {
            delay(600)
            runCatching { getAnimePreview(animeId) }.onSuccess { preview ->
                setState { copy(focusedPreview = preview) }
            }
        }
    }

    private fun launchContinueWatching(entry: WatchProgressEntry) {
        viewModelScope.launch {
            val videos = if (entry.animeId != 0)
                runCatching { getAnimeVideos(entry.animeId) }.getOrNull().orEmpty()
            else emptyList()

            val videoSources = videos.map { it.toPlayerVideoSource() }
            val targetVideo = videoSources.selectContinueWatchingVideo(
                videoId = entry.videoId,
                episodeUrl = entry.episodeUrl,
                episode = entry.episode,
                playerName = entry.playerName,
                dubbing = entry.dubbing,
            ) ?: entry.toPlayerVideoSource()
            val allVideos = videoSources.ifEmpty { listOf(targetVideo) }

            migratePlaceholderEpisode(entry, targetVideo)

            nav.navigate(
                playerNavigator.getPlayerDest(
                    video = targetVideo,
                    allVideos = allVideos,
                    animeTitle = entry.animeTitle,
                    animeId = entry.animeId,
                    posterUrl = entry.posterUrl,
                )
            )
        }
    }

    private suspend fun migratePlaceholderEpisode(entry: WatchProgressEntry, targetVideo: PlayerVideoSource) {
        if (!entry.episode.isPlaceholderEpisode() || targetVideo.episode.isPlaceholderEpisode()) return
        if (entry.episode == targetVideo.episode) return
        val isTrustedMatch = (entry.videoId > 0 && targetVideo.id == entry.videoId) ||
            (entry.episodeUrl.isNotBlank() && targetVideo.iframeUrl == entry.episodeUrl)
        if (!isTrustedMatch) return
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
