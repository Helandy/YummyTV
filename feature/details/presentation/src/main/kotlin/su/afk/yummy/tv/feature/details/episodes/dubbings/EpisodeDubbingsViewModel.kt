package su.afk.yummy.tv.feature.details.episodes.dubbings

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.settings.PreferredPlayer
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.anime.model.AnimeVideo
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.details.handler.DetailsPlayerNavigationHandler
import su.afk.yummy.tv.feature.details.utils.matchesPreferredPlayer
import su.afk.yummy.tv.feature.player.isSupportedPlayerUrl
import su.afk.yummy.tv.feature.player.playerDisplayOrderPriority

@HiltViewModel(assistedFactory = EpisodeDubbingsViewModel.Factory::class)
class EpisodeDubbingsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    @Assisted private val episode: String,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val settingsStore: SettingsStore,
    private val playerNavigationHandler: DetailsPlayerNavigationHandler,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<EpisodeDubbingsState.State, EpisodeDubbingsState.Event, EpisodeDubbingsState.Effect>(
    savedStateHandle
) {

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
                val videosByDubbing = videos
                    .asSequence()
                    .mapNotNull { video ->
                        video.dubbing.trim().takeIf { it.isNotBlank() }?.let { it to video }
                    }
                    .groupBy(keySelector = { it.first }, valueTransform = { it.second })
                val dubbings = videos
                    .asSequence()
                    .filter { it.episode == episode }
                    .map { it.dubbing.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map { dubbing ->
                        val dubbingVideos = videosByDubbing[dubbing].orEmpty()
                        EpisodeDubbingsState.DubbingItem(
                            name = dubbing,
                            views = dubbingVideos.dubbingViews(),
                            episodeCount = dubbingVideos.dubbingEpisodeCount(),
                            supportedBalancers = dubbingVideos.supportedBalancersLabel(),
                        )
                    }
                    .sortedWith(
                        compareByDescending<EpisodeDubbingsState.DubbingItem> { it.views }
                            .thenBy { it.name }
                    )
                    .toList()
                setState { copy(isLoading = false, dubbings = dubbings) }
            },
            onFailure = { e -> setState { copy(isLoading = false, error = e.message) } },
        )
    }

    private fun openDubbing(dubbingName: String) {
        viewModelScope.launch {
            val video = loadedVideos.selectLaunchVideo(
                dubbingName = dubbingName,
                preferredPlayer = settingsStore.preferredPlayer.first(),
            ) ?: return@launch
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

    private fun List<AnimeVideo>.selectLaunchVideo(
        dubbingName: String,
        preferredPlayer: PreferredPlayer,
    ): AnimeVideo? {
        val candidates = filter {
            it.episode == episode && it.dubbing.trim() == dubbingName
        }
        val supported = candidates.filter { it.iframeUrl.isSupportedPlayerUrl() }
        return supported.firstOrNull { it.iframeUrl.matchesPreferredPlayer(preferredPlayer) }
            ?: supported.minWithOrNull(
                compareBy<AnimeVideo> {
                    minOf(
                        it.player.playerDisplayOrderPriority(),
                        it.iframeUrl.playerDisplayOrderPriority(),
                    )
                }.thenBy { it.player }
            )
            ?: candidates.firstOrNull()
    }

    private fun List<AnimeVideo>.dubbingViews(): Int =
        groupBy { it.player }
            .values
            .maxOfOrNull { videos -> videos.sumOf { it.views ?: 0 } }
            ?: 0

    private fun List<AnimeVideo>.dubbingEpisodeCount(): Int =
        map { it.episode }.distinct().size

    private fun List<AnimeVideo>.supportedBalancersLabel(): String =
        asSequence()
            .filter { it.iframeUrl.isSupportedPlayerUrl() }
            .distinctBy { it.player }
            .sortedWith(
                compareBy<AnimeVideo> {
                    minOf(
                        it.player.playerDisplayOrderPriority(),
                        it.iframeUrl.playerDisplayOrderPriority(),
                    )
                }.thenBy { it.player }
            )
            .map { it.player.withoutPlayerPrefix() }
            .joinToString(" • ")

    private fun String.withoutPlayerPrefix(): String =
        removePrefix(RU_PLAYER_PREFIX).removePrefix(EN_PLAYER_PREFIX)

    private companion object {
        const val RU_PLAYER_PREFIX = "Плеер "
        const val EN_PLAYER_PREFIX = "Player "
    }
}
