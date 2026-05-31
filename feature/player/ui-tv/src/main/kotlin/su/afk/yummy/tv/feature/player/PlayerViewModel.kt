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
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStore
import su.afk.yummy.tv.domain.account.usecase.MarkVideoWatchedUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.player.extractor.AksorExtractor
import su.afk.yummy.tv.feature.player.extractor.AllohaExtractor
import su.afk.yummy.tv.feature.player.extractor.CvhExtractor
import su.afk.yummy.tv.feature.player.extractor.KodikExtractor
import su.afk.yummy.tv.feature.player.extractor.KodikResult
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
                allDubbingViews = newDest.allDubbingViews,
                allBalancerNames = newDest.allBalancerNames,
                allBalancerDubbingNames = newDest.allBalancerDubbingNames,
                allBalancerEpisodeUrls = newDest.allBalancerEpisodeUrls,
                allBalancerEpisodeNumbers = newDest.allBalancerEpisodeNumbers,
                allBalancerEpisodeVideoIds = newDest.allBalancerEpisodeVideoIds,
                allBalancerDubbingViews = newDest.allBalancerDubbingViews,
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
        allDubbingViews = dest.allDubbingViews,
        allBalancerNames = dest.allBalancerNames,
        allBalancerDubbingNames = dest.allBalancerDubbingNames,
        allBalancerEpisodeUrls = dest.allBalancerEpisodeUrls,
        allBalancerEpisodeNumbers = dest.allBalancerEpisodeNumbers,
        allBalancerEpisodeVideoIds = dest.allBalancerEpisodeVideoIds,
        allBalancerDubbingViews = dest.allBalancerDubbingViews,
        episodeSkips = dest.episodeSkips,
        allDubbingEpisodeSkips = dest.allDubbingEpisodeSkips,
        allBalancerEpisodeSkips = dest.allBalancerEpisodeSkips,
        balancerIndex = dest.currentBalancerIndex,
        dubbingIndex = dest.currentDubbingIndex,
        episodeIndex = dest.currentEpisodeIndex,
    )

    private var extractionJob: Job? = null
    private val markedWatchedVideoIds = mutableSetOf<Int>()
    private val markingWatchedVideoIds = mutableSetOf<Int>()

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
            is PlayerState.Event.PlaybackError -> {
                val detail = event.message.trim().takeIf { it.isNotBlank() }
                val message = buildString {
                    append(context.getString(R.string.player_stream_error))
                    if (detail != null) {
                        append("\n")
                        append(detail)
                    }
                }
                setState { copy(playerError = message) }
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
                val currentNum = activeEpisodeNumbers(s).getOrElse(s.episodeIndex) { "" }
                val selectedDubbing = globalDubbingNames(s).getOrElse(event.index) {
                    activeAllDubbingNames(s).getOrElse(event.index) { "" }
                }
                if (selectedDubbing.isBlank()) return
                val selection = resolveDubbingSource(
                    state = s,
                    dubbingName = selectedDubbing,
                    episodeNumber = currentNum,
                ) ?: return
                if (
                    selection.balancerIndex == s.balancerIndex &&
                    selection.dubbingIndex == s.dubbingIndex &&
                    selection.episodeIndex == s.episodeIndex
                ) return
                setState {
                    copy(
                        dubbingResumeMs = (event.currentPosMs - 3_000L).coerceAtLeast(0L),
                        balancerIndex = selection.balancerIndex,
                        dubbingIndex = selection.dubbingIndex,
                        episodeIndex = selection.episodeIndex,
                    )
                }
                loadStream()
            }
            is PlayerState.Event.BalancerSelected -> {
                val s = currentState
                if (event.index == s.balancerIndex) return
                val newDubbingNames = s.allBalancerDubbingNames.getOrElse(event.index) { emptyList() }
                val currentDubbingName = activeDubbing(s)
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
                val snapshot = event.snapshot
                if (s.animeId == 0 || snapshot.episode.isBlank() || snapshot.durationMs <= 0) return
                viewModelScope.launch {
                    watchProgressStore.save(
                        animeId = s.animeId,
                        episode = snapshot.episode,
                        videoId = snapshot.videoId,
                        episodeUrl = snapshot.episodeUrl,
                        positionMs = snapshot.positionMs,
                        durationMs = snapshot.durationMs,
                        animeTitle = s.animeTitle,
                        posterUrl = s.posterUrl,
                        playerName = snapshot.playerName,
                        dubbing = snapshot.dubbing,
                        screenshotUrl = snapshot.screenshotUrl,
                    )
                    val videoId = snapshot.videoId
                    val watchedEnough = videoId > 0 &&
                        snapshot.positionMs.toDouble() / snapshot.durationMs >= 0.9
                    if (watchedEnough) {
                        if (videoId in markedWatchedVideoIds || !markingWatchedVideoIds.add(videoId)) {
                            return@launch
                        }
                        runCatching {
                            markVideoWatched(
                                videoId = videoId,
                                timeSeconds = (snapshot.positionMs / 1000L).toInt(),
                                durationSeconds = (snapshot.durationMs / 1000L).toInt(),
                            )
                        }.onSuccess {
                            markedWatchedVideoIds += videoId
                        }.also {
                            markingWatchedVideoIds -= videoId
                        }
                    }
                }
            }
        }
    }

    private suspend fun loadResumePosition(animeId: Int, episode: String): Long? {
        val entry = watchProgressStore.get(animeId, episode) ?: return null
        return entry.positionMs.takeIf { WatchProgressStore.isContinueWatchingEntry(entry) }
    }

    private fun loadStream() {
        extractionJob?.cancel()
        extractionJob = viewModelScope.launch {
            setState {
                copy(
                    streamUrl = null,
                    streamHeaders = emptyMap(),
                    streamQualityMap = null,
                    playerError = null,
                    kodikBlockedError = null,
                    resumeFromMs = 0L,
                )
            }
            val s = currentState
            val url = activeIframeUrl(s)

            if (url.contains("alloha", ignoreCase = true)) {
                val result = AllohaExtractor.extract(url, context)
                if (result != null) {
                    val resume = consumeDubbingResume() ?: loadResumePosition(s.animeId, activeEpisode(s)) ?: 0L
                    setState {
                        copy(
                            streamHeaders = result.headers,
                            streamQualityMap = result.qualities,
                            streamUrl = result.url,
                            resumeFromMs = resume,
                        )
                    }
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
                val result = AksorExtractor.extract(url)
                if (result != null) {
                    val resume = consumeDubbingResume() ?: loadResumePosition(s.animeId, activeEpisode(s)) ?: 0L
                    setState {
                        copy(
                            streamHeaders = result.headers,
                            streamQualityMap = result.qualities,
                            streamUrl = result.url,
                            resumeFromMs = resume,
                        )
                    }
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
                    setState { copy(streamQualityMap = qualities, streamUrl = qualities.values.last(), resumeFromMs = resume) }
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

    private fun globalDubbingNames(s: PlayerState.State): List<String> =
        if (s.allBalancerDubbingNames.isNotEmpty()) {
            s.allBalancerDubbingNames.flatten().distinct()
        } else {
            s.allDubbingNames
        }

    private fun resolveDubbingSource(
        state: PlayerState.State,
        dubbingName: String,
        episodeNumber: String,
    ): DubbingSource? {
        if (state.allBalancerDubbingNames.isEmpty()) {
            val dubbingIndex = state.allDubbingNames.indexOf(dubbingName).takeIf { it >= 0 } ?: return null
            val episodeNumbers = state.allDubbingEpisodeNumbers.getOrElse(dubbingIndex) { emptyList() }
            val episodeIndex = episodeNumbers.indexOf(episodeNumber).takeIf { it >= 0 } ?: 0
            return DubbingSource(
                balancerIndex = state.balancerIndex,
                dubbingIndex = dubbingIndex,
                episodeIndex = episodeIndex,
            )
        }

        val candidates = state.allBalancerDubbingNames.flatMapIndexed { balancerIndex, dubbingNames ->
            dubbingNames.mapIndexedNotNull { dubbingIndex, name ->
                if (name != dubbingName) return@mapIndexedNotNull null
                val episodeNumbers = state.allBalancerEpisodeNumbers
                    .getOrElse(balancerIndex) { emptyList() }
                    .getOrElse(dubbingIndex) { emptyList() }
                val episodeIndex = episodeNumbers.indexOf(episodeNumber).takeIf { it >= 0 } ?: 0
                DubbingSource(
                    balancerIndex = balancerIndex,
                    dubbingIndex = dubbingIndex,
                    episodeIndex = episodeIndex,
                )
            }
        }

        return candidates.firstOrNull { it.balancerIndex == state.balancerIndex }
            ?: candidates.firstOrNull()
    }

    private data class DubbingSource(
        val balancerIndex: Int,
        val dubbingIndex: Int,
        val episodeIndex: Int,
    )
}
