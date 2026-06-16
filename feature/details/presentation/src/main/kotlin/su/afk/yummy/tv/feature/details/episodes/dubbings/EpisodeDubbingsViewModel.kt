package su.afk.yummy.tv.feature.details.episodes.dubbings

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics

@HiltViewModel(assistedFactory = EpisodeDubbingsViewModel.Factory::class)
class EpisodeDubbingsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    @Assisted private val episode: String,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val analytics: DetailsAnalytics,
) : BaseViewModelNew<EpisodeDubbingsState.State, EpisodeDubbingsState.Event, EpisodeDubbingsState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int, episode: String): EpisodeDubbingsViewModel
    }

    override fun createInitialState() = EpisodeDubbingsState.State(episode = episode)

    init {
        analytics.eventEpisodeDubbingsScreenOpened(animeId)
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: EpisodeDubbingsState.Event) {
        when (event) {
            EpisodeDubbingsState.Event.BackSelected -> nav.back()
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                val dubbings = videos
                    .asSequence()
                    .filter { it.episode == episode }
                    .map { it.dubbing.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .sorted()
                    .toList()
                setState { copy(isLoading = false, dubbings = dubbings) }
            },
            onFailure = { e -> setState { copy(isLoading = false, error = e.message) } },
        )
    }
}
