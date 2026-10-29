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
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.settings.PreferredPlayer
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.PlayerSettingsStore
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.details.model.DetailsPlayerSelection
import su.afk.yummy.tv.feature.details.mapper.episodeDubbingItems
import su.afk.yummy.tv.feature.details.details.handler.SourceStarsHandler
import su.afk.yummy.tv.feature.details.utils.playerPosterUrl
import su.afk.yummy.tv.feature.details.utils.withDubbingStars
import su.afk.yummy.tv.feature.details.utils.withStars
import su.afk.yummy.tv.feature.details.utils.screenshotByEpisode

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
    private val sourceStars: SourceStarsHandler,
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

    private var starredDubbings: Set<String> = emptySet()
    private var starredBalancers: Set<String> = emptySet()

    init {
        analytics.eventEpisodeDubbingsScreenOpened(animeId)
        observeSourceStars()
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

            is EpisodeDubbingsState.Event.DubbingStarToggled ->
                viewModelScope.launch { sourceStars.toggleDubbing(event.name) }

            is EpisodeDubbingsState.Event.BalancerStarToggled ->
                viewModelScope.launch { sourceStars.toggleBalancer(event.playerName) }
        }
    }

    private fun observeSourceStars() {
        viewModelScope.launch {
            sourceStars.starredDubbings.collect { starred ->
                starredDubbings = starred
                setState { copy(dubbings = dubbings.withDubbingStars(starred).toImmutableList()) }
            }
        }
        viewModelScope.launch {
            sourceStars.starredBalancers.collect { starred ->
                starredBalancers = starred
                setState { copy(pendingBalancerSelection = pendingBalancerSelection?.withStars(starred)) }
            }
        }
    }

    private suspend fun loadMeta() {
        runSuspendCatching { getAnimeDetails(animeId) }.onSuccess { details ->
            animeTitle = details.title
            posterUrl = details.playerPosterUrl()
            screenshotsByEpisode = details.screenshotByEpisode()
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runSuspendCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                loadedVideos = videos
                val dubbings = videos.episodeDubbingItems(episode).withDubbingStars(starredDubbings)
                setState { copy(isLoading = false, dubbings = dubbings.toImmutableList()) }
            },
            onFailure = { e -> setState { copy(isLoading = false, error = UiText.Failure(e)) } },
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
                    rememberedAnime4kDubbing = settingsStore.anime4kDubbing(animeId).first(),
                )
            ) {
                is DetailsPlayerSelection.Navigate -> navigateToPlayer(selection.video)
                is DetailsPlayerSelection.ShowPicker ->
                    setState { copy(pendingBalancerSelection = selection.picker.withStars(starredBalancers)) }
            }
        }
    }

    private fun navigateToPlayer(video: AnimeVideo) {
        viewModelScope.launch {
            if (!playerNavigationHandler.confirmOpen(video)) return@launch
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
