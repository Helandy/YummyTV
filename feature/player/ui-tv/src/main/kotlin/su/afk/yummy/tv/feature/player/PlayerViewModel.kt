package su.afk.yummy.tv.feature.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.storage.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.MarkVideoWatchedUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.extractor.AksorExtractor
import su.afk.yummy.tv.feature.player.extractor.AllohaExtractor
import su.afk.yummy.tv.feature.player.extractor.CvhExtractor
import su.afk.yummy.tv.feature.player.extractor.KodikExtractor
import su.afk.yummy.tv.feature.player.extractor.KodikResult
import su.afk.yummy.tv.feature.player.extractor.YouTubeExtractor
import su.afk.yummy.tv.feature.player.navigator.PlayerDestination

@HiltViewModel(assistedFactory = PlayerViewModel.Factory::class)
class PlayerViewModel @AssistedInject constructor(
    @Assisted private val dest: PlayerDestination,
    @param:ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val watchProgressStore: WatchProgressStore,
    private val settingsStore: SettingsStore,
    private val markVideoWatched: MarkVideoWatchedUseCase,
    private val detailsNavigator: IDetailsNavigator,
) : BaseViewModelNew<PlayerState.State, PlayerState.Event, PlayerState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(dest: PlayerDestination): PlayerViewModel
    }

    private var activeDest: PlayerDestination = dest

    fun loadDestination(newDest: PlayerDestination) {
        if (newDest == activeDest) return
        activeDest = newDest
        setState {
            PlayerState.State(
                iframeUrl = newDest.iframeUrl,
                animeTitle = newDest.animeTitle,
                episode = newDest.episode,
                playerName = newDest.playerName,
                dubbing = newDest.dubbing,
                episodeUrls = newDest.episodeUrls,
                episodeNumbers = newDest.episodeNumbers,
                episodeVideoIds = newDest.episodeVideoIds,
                screenshotUrls = newDest.screenshotUrls,
                animeId = newDest.animeId,
                posterUrl = newDest.posterUrl,
                allDubbingNames = newDest.allDubbingNames,
                allDubbingEpisodeUrls = newDest.allDubbingEpisodeUrls,
                allDubbingEpisodeNumbers = newDest.allDubbingEpisodeNumbers,
                allDubbingEpisodeVideoIds = newDest.allDubbingEpisodeVideoIds,
                allBalancerNames = newDest.allBalancerNames,
                allBalancerDubbingNames = newDest.allBalancerDubbingNames,
                allBalancerEpisodeUrls = newDest.allBalancerEpisodeUrls,
                allBalancerEpisodeNumbers = newDest.allBalancerEpisodeNumbers,
                allBalancerEpisodeVideoIds = newDest.allBalancerEpisodeVideoIds,
                episodeSkips = newDest.episodeSkips,
                allDubbingEpisodeSkips = newDest.allDubbingEpisodeSkips,
                allBalancerEpisodeSkips = newDest.allBalancerEpisodeSkips,
                balancerIndex = newDest.currentBalancerIndex,
                dubbingIndex = newDest.currentDubbingIndex,
                episodeIndex = newDest.currentEpisodeIndex,
            )
        }
        loadStream()
    }

    override fun createInitialState() = PlayerState.State(
        iframeUrl = dest.iframeUrl,
        animeTitle = dest.animeTitle,
        episode = dest.episode,
        playerName = dest.playerName,
        dubbing = dest.dubbing,
        episodeUrls = dest.episodeUrls,
        episodeNumbers = dest.episodeNumbers,
        episodeVideoIds = dest.episodeVideoIds,
        screenshotUrls = dest.screenshotUrls,
        animeId = dest.animeId,
        posterUrl = dest.posterUrl,
        allDubbingNames = dest.allDubbingNames,
        allDubbingEpisodeUrls = dest.allDubbingEpisodeUrls,
        allDubbingEpisodeNumbers = dest.allDubbingEpisodeNumbers,
        allDubbingEpisodeVideoIds = dest.allDubbingEpisodeVideoIds,
        allBalancerNames = dest.allBalancerNames,
        allBalancerDubbingNames = dest.allBalancerDubbingNames,
        allBalancerEpisodeUrls = dest.allBalancerEpisodeUrls,
        allBalancerEpisodeNumbers = dest.allBalancerEpisodeNumbers,
        allBalancerEpisodeVideoIds = dest.allBalancerEpisodeVideoIds,
        episodeSkips = dest.episodeSkips,
        allDubbingEpisodeSkips = dest.allDubbingEpisodeSkips,
        allBalancerEpisodeSkips = dest.allBalancerEpisodeSkips,
        balancerIndex = dest.currentBalancerIndex,
        dubbingIndex = dest.currentDubbingIndex,
        episodeIndex = dest.currentEpisodeIndex,
    )

    private var extractionJob: Job? = null

    init {
        settingsStore.autoSkipOpeningsEndings
            .onEach { enabled -> setState { copy(autoSkipOpeningsEndings = enabled) } }
            .launchIn(viewModelScope)
        loadStream()
    }

    override fun onEvent(event: PlayerState.Event) {
        when (event) {
            PlayerState.Event.Back -> nav.back()
            PlayerState.Event.RetryStream -> {
                setState { copy(retryKey = retryKey + 1) }
                loadStream()
            }
            PlayerState.Event.RateTitle -> {
                val animeId = currentState.animeId
                if (animeId > 0) nav.navigate(detailsNavigator.getRatingDest(animeId))
            }
            PlayerState.Event.PrevEpisode -> {
                val idx = currentState.episodeIndex
                if (idx > 0) {
                    setState { copy(episodeIndex = idx - 1) }
                    loadStream()
                }
            }
            PlayerState.Event.NextEpisode -> {
                val s = currentState
                val urls = activeDubbingUrls(s)
                if (s.episodeIndex < urls.size - 1) {
                    setState { copy(episodeIndex = s.episodeIndex + 1) }
                    loadStream()
                }
            }
            is PlayerState.Event.DubbingSelected -> {
                val s = currentState
                if (event.index == s.dubbingIndex) return
                val currentNum = activeEpisodeNumbers(s).getOrElse(s.episodeIndex) { "" }
                val newNums = activeAllEpisodeNumbers(s).getOrElse(event.index) { emptyList() }
                val newEpisodeIdx = newNums.indexOf(currentNum).takeIf { it >= 0 } ?: 0
                setState {
                    copy(
                        dubbingResumeMs = (event.currentPosMs - 3_000L).coerceAtLeast(0L),
                        dubbingIndex = event.index,
                        episodeIndex = newEpisodeIdx,
                    )
                }
                loadStream()
            }
            is PlayerState.Event.BalancerSelected -> {
                val s = currentState
                if (event.index == s.balancerIndex) return
                val newDubbingNames = s.allBalancerDubbingNames.getOrElse(event.index) { emptyList() }
                val currentDubbingName = s.allDubbingNames.getOrElse(s.dubbingIndex) { s.dubbing }
                val newDubbingIdx = newDubbingNames.indexOf(currentDubbingName).takeIf { it >= 0 } ?: 0
                val currentEpNum = activeEpisodeNumbers(s).getOrElse(s.episodeIndex) { s.episode }
                val newEpNums = s.allBalancerEpisodeNumbers.getOrElse(event.index) { emptyList() }
                    .getOrElse(newDubbingIdx) { emptyList() }
                val newEpisodeIdx = newEpNums.indexOf(currentEpNum).takeIf { it >= 0 } ?: 0
                setState {
                    copy(
                        dubbingResumeMs = (event.currentPosMs - 3_000L).coerceAtLeast(0L),
                        balancerIndex = event.index,
                        dubbingIndex = newDubbingIdx,
                        episodeIndex = newEpisodeIdx,
                    )
                }
                loadStream()
            }
            is PlayerState.Event.SaveProgress -> {
                val s = currentState
                val episode = activeEpisode(s)
                if (s.animeId == 0 || episode.isBlank() || event.durMs <= 0) return
                viewModelScope.launch {
                    watchProgressStore.save(
                        animeId = s.animeId,
                        episode = episode,
                        videoId = activeVideoId(s),
                        episodeUrl = activeIframeUrl(s),
                        positionMs = event.posMs,
                        durationMs = event.durMs,
                        animeTitle = s.animeTitle,
                        posterUrl = s.posterUrl,
                        playerName = activeBalancerName(s),
                        dubbing = activeDubbing(s),
                        screenshotUrl = activeScreenshotUrl(s),
                    )
                    val videoId = activeVideoId(s)
                    val watchedEnough = videoId > 0 && event.durMs > 0 && event.posMs.toDouble() / event.durMs >= 0.9
                    if (watchedEnough) {
                        runCatching {
                            markVideoWatched(
                                videoId = videoId,
                                timeSeconds = (event.posMs / 1000L).toInt(),
                                durationSeconds = (event.durMs / 1000L).toInt(),
                            )
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadResumePosition(animeId: Int, episode: String): Long? {
        val entry = watchProgressStore.get(animeId, episode) ?: return null
        if (entry.durationMs <= 0) return null
        val progress = entry.positionMs.toFloat() / entry.durationMs
        return when {
            entry.positionMs < 30_000 -> null
            progress > 0.90f -> null
            else -> entry.positionMs
        }
    }

    private fun loadStream() {
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            setState {
                copy(
                    streamUrl = null,
                    streamHeaders = emptyMap(),
                    cvhQualityMap = null,
                    youtubeWebViewFallback = false,
                    playerError = null,
                    kodikBlockedError = null,
                    resumeFromMs = 0L,
                )
            }
            val s = currentState
            val url = activeIframeUrl(s)

            if (url.contains("youtube.com", ignoreCase = true)) {
                val extracted = YouTubeExtractor.extract(url)
                if (extracted != null) {
                    setState { copy(streamUrl = extracted) }
                } else {
                    setState { copy(youtubeWebViewFallback = true) }
                }
                return@launch
            }

            if (url.contains("alloha", ignoreCase = true)) {
                val result = AllohaExtractor.extract(url, context)
                if (result != null) {
                    val resume = consumeDubbingResume() ?: loadResumePosition(s.animeId, activeEpisode(s)) ?: 0L
                    setState { copy(streamHeaders = result.headers, streamUrl = result.url, resumeFromMs = resume) }
                } else {
                    setState { copy(playerError = context.getString(R.string.player_stream_error)) }
                }
                return@launch
            }

            if (url.contains("kodik", ignoreCase = true)) {
                when (val result = KodikExtractor.extract(
                    iframeUrl = url,
                    blockedFallback = context.getString(R.string.player_kodik_blocked),
                    serverErrorMessage = { code -> context.getString(R.string.player_server_error, code) },
                )) {
                    is KodikResult.Stream -> {
                        val resume = consumeDubbingResume() ?: loadResumePosition(s.animeId, activeEpisode(s)) ?: 0L
                        setState { copy(streamUrl = result.url, resumeFromMs = resume) }
                    }
                    is KodikResult.Blocked -> setState { copy(kodikBlockedError = result.message) }
                    KodikResult.Failed -> setState { copy(playerError = context.getString(R.string.player_stream_error)) }
                }
                return@launch
            }

            if (url.contains("aksor.tv", ignoreCase = true)) {
                val extracted = AksorExtractor.extract(url)
                if (extracted != null) {
                    val resume = consumeDubbingResume() ?: loadResumePosition(s.animeId, activeEpisode(s)) ?: 0L
                    setState { copy(streamUrl = extracted, resumeFromMs = resume) }
                } else {
                    setState { copy(playerError = context.getString(R.string.player_stream_error)) }
                }
                return@launch
            }

            if (url.contains("iframeCVH", ignoreCase = true) ||
                url.contains("yummyani.me", ignoreCase = true)
            ) {
                val qualities = CvhExtractor.extract(url, context.getString(R.string.player_quality_auto))
                if (qualities != null) {
                    val resume = consumeDubbingResume() ?: loadResumePosition(s.animeId, activeEpisode(s)) ?: 0L
                    setState { copy(cvhQualityMap = qualities, streamUrl = qualities.values.last(), resumeFromMs = resume) }
                } else {
                    setState { copy(playerError = context.getString(R.string.player_stream_error)) }
                }
                return@launch
            }

            setState { copy(playerError = context.getString(R.string.player_unsupported)) }
        }
    }

    private fun consumeDubbingResume(): Long? {
        val pending = currentState.dubbingResumeMs
        return if (pending >= 0L) {
            setState { copy(dubbingResumeMs = -1L) }
            pending
        } else null
    }

    // Derived helpers (mirror the composable's derivations)
    private fun activeAllDubbingNames(s: PlayerState.State) =
        if (s.allBalancerDubbingNames.isNotEmpty())
            s.allBalancerDubbingNames.getOrElse(s.balancerIndex) { s.allDubbingNames }
        else s.allDubbingNames

    private fun activeAllEpisodeUrls(s: PlayerState.State) =
        if (s.allBalancerEpisodeUrls.isNotEmpty())
            s.allBalancerEpisodeUrls.getOrElse(s.balancerIndex) { s.allDubbingEpisodeUrls }
        else s.allDubbingEpisodeUrls

    private fun activeAllEpisodeNumbers(s: PlayerState.State) =
        if (s.allBalancerEpisodeNumbers.isNotEmpty())
            s.allBalancerEpisodeNumbers.getOrElse(s.balancerIndex) { s.allDubbingEpisodeNumbers }
        else s.allDubbingEpisodeNumbers

    private fun activeAllEpisodeVideoIds(s: PlayerState.State) =
        if (s.allBalancerEpisodeVideoIds.isNotEmpty())
            s.allBalancerEpisodeVideoIds.getOrElse(s.balancerIndex) { s.allDubbingEpisodeVideoIds }
        else s.allDubbingEpisodeVideoIds

    private fun activeAllEpisodeSkips(s: PlayerState.State) =
        if (s.allBalancerEpisodeSkips.isNotEmpty())
            s.allBalancerEpisodeSkips.getOrElse(s.balancerIndex) { s.allDubbingEpisodeSkips }
        else s.allDubbingEpisodeSkips

    private fun activeDubbingUrls(s: PlayerState.State) =
        activeAllEpisodeUrls(s).getOrElse(s.dubbingIndex) { s.episodeUrls }

    private fun activeEpisodeNumbers(s: PlayerState.State) =
        activeAllEpisodeNumbers(s).getOrElse(s.dubbingIndex) { s.episodeNumbers }

    private fun activeEpisodeSkipsList(s: PlayerState.State) =
        activeAllEpisodeSkips(s).getOrElse(s.dubbingIndex) { s.episodeSkips }

    private fun activeEpisodeVideoIds(s: PlayerState.State) =
        activeAllEpisodeVideoIds(s).getOrElse(s.dubbingIndex) { s.episodeVideoIds }

    private fun activeIframeUrl(s: PlayerState.State) =
        activeDubbingUrls(s).getOrElse(s.episodeIndex) { s.iframeUrl }

    private fun activeEpisode(s: PlayerState.State) =
        activeEpisodeNumbers(s).getOrElse(s.episodeIndex) { s.episode }

    private fun activeVideoId(s: PlayerState.State) =
        activeEpisodeVideoIds(s).getOrElse(s.episodeIndex) { 0 }

    private fun activeDubbing(s: PlayerState.State) =
        activeAllDubbingNames(s).getOrElse(s.dubbingIndex) { s.dubbing }

    private fun activeBalancerName(s: PlayerState.State) =
        if (s.allBalancerNames.isNotEmpty())
            s.allBalancerNames.getOrElse(s.balancerIndex) { s.playerName }
        else s.playerName

    private fun activeScreenshotUrl(s: PlayerState.State) =
        s.screenshotUrls.getOrElse(s.episodeIndex) { "" }
}
