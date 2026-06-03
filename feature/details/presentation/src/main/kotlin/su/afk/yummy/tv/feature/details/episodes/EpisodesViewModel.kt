package su.afk.yummy.tv.feature.details.episodes

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.details.BalancerOption
import su.afk.yummy.tv.feature.details.details.BalancerPickerState
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.utils.toPlayerSkips
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.PlayerVideoSource
import su.afk.yummy.tv.feature.player.getPlayerDest

@HiltViewModel(assistedFactory = EpisodesViewModel.Factory::class)
class EpisodesViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val playerNavigator: IPlayerNavigator,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
) : BaseViewModelNew<EpisodesState.State, EpisodesState.Event, EpisodesState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): EpisodesViewModel
    }

    override fun createInitialState() = EpisodesState.State()

    private val preferredPlayerState = settingsStore.preferredPlayer.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = PreferredPlayer.NONE,
    )

    private var animeTitle = ""
    private var posterUrl = ""
    private var screenshotsByEpisode: Map<String, String> = emptyMap()

    init {
        viewModelScope.launch { loadMeta() }
        viewModelScope.launch { loadVideos() }
        watchProgressStore.observeByAnimeId(animeId)
            .map { entries -> entries.associateBy { it.episodeUrl } }
            .flowOn(Dispatchers.Default)
            .onEach { progress -> setState { copy(watchProgress = progress) } }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: EpisodesState.Event) {
        when (event) {
            EpisodesState.Event.BackSelected -> nav.back()
            is EpisodesState.Event.VideoSelected -> showBalancerPicker(event.video)
            is EpisodesState.Event.BalancerConfirmed -> {
                setState { copy(pendingBalancerSelection = null) }
                navigateToPlayer(event.video)
            }
            EpisodesState.Event.BalancerPickerDismissed ->
                setState { copy(pendingBalancerSelection = null) }
        }
    }

    private suspend fun loadMeta() {
        runCatching { getAnimeDetails(animeId) }.onSuccess { details ->
            animeTitle = details.title
            posterUrl = details.poster?.run { medium ?: big ?: fullsize ?: small } ?: ""
            screenshotsByEpisode = details.screenshots
                .mapNotNull { s -> s.episode?.let { ep -> ep to (s.small ?: "") } }
                .toMap()
        }
    }

    private suspend fun loadVideos() {
        setState { copy(videosState = VideosUiState.Loading) }
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                setState {
                    copy(videosState = if (videos.isEmpty()) VideosUiState.Empty else VideosUiState.Content(videos))
                }
            },
            onFailure = { setState { copy(videosState = VideosUiState.Empty) } },
        )
    }

    private fun isSupportedPlayer(iframeUrl: String): Boolean =
        iframeUrl.contains("kodik", ignoreCase = true) ||
        iframeUrl.contains("aksor.tv", ignoreCase = true) ||
        iframeUrl.contains("iframeCVH", ignoreCase = true) ||
        iframeUrl.contains("alloha", ignoreCase = true)

    private fun matchesPreferredPlayer(iframeUrl: String, preferred: PreferredPlayer): Boolean =
        when (preferred) {
            PreferredPlayer.NONE -> false
            PreferredPlayer.KODIK -> iframeUrl.contains("kodik", ignoreCase = true)
            PreferredPlayer.AKSOR -> iframeUrl.contains("aksor.tv", ignoreCase = true)
            PreferredPlayer.ALLOHA -> iframeUrl.contains("alloha", ignoreCase = true)
            PreferredPlayer.CVH -> iframeUrl.contains("iframeCVH", ignoreCase = true)
        }

    private fun showBalancerPicker(video: AnimeVideo) {
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        val options = allVideos
            .filter { it.episode == video.episode }
            .groupBy { it.player }
            .entries
            .map { (playerName, playerVideos) ->
                val supported = isSupportedPlayer(playerVideos.first().iframeUrl)
                val rep = playerVideos.firstOrNull { it.dubbing == video.dubbing }
                    ?: playerVideos.maxByOrNull { it.views ?: 0 }
                    ?: playerVideos.first()
                BalancerOption(playerName = playerName, video = rep, isSupported = supported)
            }
        val supportedOptions = options.filter { it.isSupported }

        val preferredPlayer = preferredPlayerState.value
        if (preferredPlayer != PreferredPlayer.NONE) {
            val preferred = supportedOptions.firstOrNull { matchesPreferredPlayer(it.video.iframeUrl, preferredPlayer) }
            if (preferred != null) {
                navigateToPlayer(preferred.video)
                return
            }
        }

        when (supportedOptions.size) {
            0 -> navigateToPlayer(video)
            1 -> navigateToPlayer(supportedOptions.first().video)
            else -> setState { copy(pendingBalancerSelection = BalancerPickerState(video.episode, options)) }
        }
    }

    private fun navigateToPlayer(video: AnimeVideo) {
        val allVideos = (currentState.videosState as? VideosUiState.Content)?.videos ?: return
        val title = animeTitle
        val poster = posterUrl
        val screenshots = screenshotsByEpisode
        viewModelScope.launch(Dispatchers.Default) {
            val destination = playerNavigator.getPlayerDest(
                video = video.toPlayerVideoSource(),
                allVideos = allVideos.map { it.toPlayerVideoSource() },
                animeTitle = title,
                animeId = animeId,
                posterUrl = poster,
                screenshotByEpisode = screenshots,
            )
            withContext(Dispatchers.Main) { nav.navigate(destination) }
        }
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
