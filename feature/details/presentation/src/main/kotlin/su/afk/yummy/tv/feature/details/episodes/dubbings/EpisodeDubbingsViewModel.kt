package su.afk.yummy.tv.feature.details.episodes.dubbings

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.details.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.mapper.episodeDubbingItems

@HiltViewModel(assistedFactory = EpisodeDubbingsViewModel.Factory::class)
class EpisodeDubbingsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    @Assisted private val episode: String,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val settingsStore: PlayerSettingsStore,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<EpisodeDubbingsState.State, EpisodeDubbingsState.Event, EpisodeDubbingsState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int, episode: String): EpisodeDubbingsViewModel
    }

    override fun createInitialState() = EpisodeDubbingsState.State(episode = episode)

    private var animeTitle = ""
    private var posterUrl = ""
    private var screenshotsByEpisode: Map<String, String> = emptyMap()
    private var loadedVideos: List<AnimeVideo> = emptyList()

    init {
        analytics.eventEpisodeDubbingsScreenOpened(animeId)
        viewModelScope.launch { loadMeta() }
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: EpisodeDubbingsState.Event) {
        when (event) {
            EpisodeDubbingsState.Event.BackSelected -> nav.back()
            is EpisodeDubbingsState.Event.DubbingSelected -> openDubbing(event.name)
            EpisodeDubbingsState.Event.RetrySelected -> viewModelScope.launch { load() }
            is EpisodeDubbingsState.Event.BalancerConfirmed -> {
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
            }

            EpisodeDubbingsState.Event.BalancerPickerDismissed ->
                setState { copy(pendingBalancerSelection = null) }
        }
    }

    private suspend fun loadMeta() {
        runCatching { getAnimeDetails(animeId) }.onSuccess { details ->
            animeTitle = details.title
            posterUrl = details.poster?.run { medium ?: big ?: fullsize ?: small } ?: ""
            screenshotsByEpisode = details.screenshots
                .mapNotNull { screenshot ->
                    screenshot.episode?.let { episode -> episode to (screenshot.small ?: "") }
                }
                .toMap()
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                loadedVideos = videos
                val dubbings = videos.episodeDubbingItems(episode)
                setState { copy(isLoading = false, dubbings = dubbings.toImmutableList()) }
            },
            onFailure = { e -> setState { copy(isLoading = false, error = e.userMessage()) } },
        )
    }

    private fun openDubbing(dubbingName: String) {
        viewModelScope.launch {
            val dubbingVideos = loadedVideos.filter { it.dubbing.trim() == dubbingName }
            val candidate = dubbingVideos.selectEpisodeDubbingLaunchVideo(
                episode = episode,
                dubbingName = dubbingName,
                preferredPlayer = PreferredPlayer.NONE,
            ) ?: return@launch
            when (
                val selection = playerNavigationHandler.selectPlayer(
                    video = candidate,
                    allVideos = dubbingVideos,
                    preferredPlayer = settingsStore.preferredPlayer.first(),
                )
            ) {
                is DetailsPlayerSelection.Navigate -> navigateToPlayer(selection.video)
                is DetailsPlayerSelection.ShowPicker ->
                    setState { copy(pendingBalancerSelection = selection.picker) }
            }
        }
    }

    private fun navigateToPlayer(video: AnimeVideo) {
        viewModelScope.launch {
            val destination = withContext(Dispatchers.Default) {
                playerNavigationHandler.getPlayerDestination(
                    video = video,
                    animeTitle = animeTitle,
                    animeId = animeId,
                    posterUrl = posterUrl,
                    screenshotByEpisode = screenshotsByEpisode,
                )
            }
            nav.navigate(destination)
        }
    }

}
