package su.afk.yummy.tv.feature.library

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.storage.library.LibraryEntry
import su.afk.yummy.tv.core.storage.library.LibraryStore
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.UserAnimeList
import su.afk.yummy.tv.domain.account.UserAnimeListItem
import su.afk.yummy.tv.domain.account.UserListsRepository
import su.afk.yummy.tv.domain.account.VideoWatchesRepository
import su.afk.yummy.tv.domain.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.domain.anime.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.GetAnimePreviewUseCase
import su.afk.yummy.tv.domain.anime.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerSkipSegment
import su.afk.yummy.tv.feature.player.PlayerSkips
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val libraryStore: LibraryStore,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val userListsRepository: UserListsRepository,
    private val videoWatchesRepository: VideoWatchesRepository,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val playerNavigator: IPlayerNavigator,
    private val getAnimePreview: GetAnimePreviewUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
) : BaseViewModelNew<LibraryState.State, LibraryState.Event, LibraryState.Effect>(savedStateHandle) {

    override fun createInitialState() = LibraryState.State()

    private var previewJob: Job? = null

    init {
        libraryStore.observeAll()
            .onEach { entries -> setState { copy(items = entries) } }
            .launchIn(viewModelScope)

        watchProgressStore.observeAll()
            .map { entries ->
                entries
                    .filter { it.animeId > 0 }
                    .groupBy { it.animeId }
                    .values
                    .map { group -> group.maxBy { it.updatedAt } }
                    .sortedByDescending { it.updatedAt }
            }
            .onEach { entries -> setState { copy(continueWatching = entries) } }
            .launchIn(viewModelScope)
        settingsStore.yaniUserId
            .onEach { userId ->
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
            is LibraryState.Event.RemoveLibraryEntry ->
                viewModelScope.launch { libraryStore.remove(event.animeId) }
            is LibraryState.Event.RemoveWatchProgress ->
                viewModelScope.launch {
                    val entries = currentState.continueWatching.filter { it.animeId == event.animeId }
                    watchProgressStore.deleteByAnimeId(event.animeId)
                    entries.mapNotNull { it.videoId.takeIf { id -> id > 0 } }.distinct().forEach { videoId ->
                        runCatching { videoWatchesRepository.removeWatched(videoId) }
                    }
                }
            is LibraryState.Event.RemoveRemoteEntry -> removeRemoteEntry(event)
        }
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

    private suspend fun fetchRemoteLists(userId: Int): Map<LibraryTab, List<UserAnimeListItem>> =
        mapOf(
            LibraryTab.WATCHING to userListsRepository.getUserList(userId, UserAnimeList.WATCHING),
            LibraryTab.PLANNED to userListsRepository.getUserList(userId, UserAnimeList.PLANNED),
            LibraryTab.COMPLETED to userListsRepository.getUserList(userId, UserAnimeList.COMPLETED),
            LibraryTab.POSTPONED to userListsRepository.getUserList(userId, UserAnimeList.POSTPONED),
            LibraryTab.DROPPED to userListsRepository.getUserList(userId, UserAnimeList.DROPPED),
        )

    private suspend fun syncLocalMissingToRemote(
        remote: Map<LibraryTab, List<UserAnimeListItem>>,
    ): LocalLibrarySyncResult {
        val remoteAnimeIds = remote.values.flatten().map { it.animeId }.toSet()
        val localMissing = libraryStore.getAll().filterNot { it.animeId in remoteAnimeIds }
        var syncedAny = false
        var firstError: String? = null

        localMissing.forEach { entry ->
            val list = entry.userAnimeList()
            runCatching {
                userListsRepository.setAnimeList(entry.animeId, list)
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
                if (event.favorite) userListsRepository.setFavorite(event.animeId, false) else userListsRepository.removeAnimeList(event.animeId)
            }
            if (result.isFailure) setState { copy(remoteItems = previous, remoteError = result.exceptionOrNull()?.message) }
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
            val skips = episodeGroup.map { it.skips.toPlayerSkips() }.ifEmpty { listOf(PlayerSkips.Empty) }
            val idx = urls.indexOf(iframeUrl).coerceAtLeast(0)

            nav.navigate(
                playerNavigator.getPlayerDest(
                    iframeUrl = iframeUrl,
                    animeTitle = entry.animeTitle,
                    episode = entry.episode,
                    playerName = resolvedPlayer,
                    dubbing = resolvedDubbing,
                    episodeUrls = urls,
                    episodeNumbers = numbers,
                    episodeVideoIds = episodeGroup.map { it.id }.ifEmpty { listOf(entry.videoId) },
                    currentEpisodeIndex = idx,
                    episodeSkips = skips,
                    animeId = entry.animeId,
                    posterUrl = entry.posterUrl,
                )
            )
        }
    }
}

private data class LocalLibrarySyncResult(
    val syncedAny: Boolean,
    val error: String?,
)

private fun LibraryEntry.userAnimeList(): UserAnimeList =
    UserAnimeList.entries.firstOrNull { it.id == listId } ?: UserAnimeList.WATCHING

private fun AnimeVideoSkips.toPlayerSkips(): PlayerSkips = PlayerSkips(
    opening = opening.toPlayerSkipSegment(),
    ending = ending.toPlayerSkipSegment(),
)

private fun AnimeVideoSkipSegment?.toPlayerSkipSegment(): PlayerSkipSegment? =
    this?.let { PlayerSkipSegment(startMs = it.startMs, endMs = it.endMs) }
