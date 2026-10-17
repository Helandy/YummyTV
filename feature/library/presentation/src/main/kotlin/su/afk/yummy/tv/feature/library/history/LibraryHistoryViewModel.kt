package su.afk.yummy.tv.feature.library.history

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.core.utils.paging.pagingFlow
import su.afk.yummy.tv.domain.library.usecase.GetWatchHistoryPageUseCase
import su.afk.yummy.tv.domain.player.usecase.GetMeaningfulVideoProgressUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.library.utils.historyProgressKey
import javax.inject.Inject

/**
 * История просмотров отдельным экраном. Своя VM, а не LibraryViewModel: та при каждом создании
 * и возврате на экран синхронизирует списки с сервером, истории это не нужно.
 */
@HiltViewModel
class LibraryHistoryViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val settingsStore: SettingsStore,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getWatchHistoryPage: GetWatchHistoryPageUseCase,
    private val getMeaningfulVideoProgress: GetMeaningfulVideoProgressUseCase,
) : BaseViewModel<LibraryHistoryState.State, LibraryHistoryState.Event, LibraryHistoryState.Effect>() {

    override fun createInitialState() = LibraryHistoryState.State(
        watchHistory = pagingFlow(
            viewModelScope,
            pageSize = 100,
            // Тот же ключ, что в библиотеке: пересмотры дают повторы animeId+серия.
            itemKey = { "${it.animeId}:${it.episode}:${it.watchedAtSeconds}" },
        ) { limit, offset ->
            getWatchHistoryPage(limit, offset)
        },
    )

    init {
        settingsStore.yaniUserId
            .onEach { userId -> setState { copy(isSignedIn = userId > 0) } }
            .launchIn(viewModelScope)
        loadLocalProgress()
    }

    override fun onEvent(event: LibraryHistoryState.Event) {
        when (event) {
            is LibraryHistoryState.Event.EntrySelected ->
                nav.navigate(detailsNavigator.getEpisodesDest(event.entry.animeId, event.entry.episode))

            is LibraryHistoryState.Event.DetailsSelected ->
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))

            LibraryHistoryState.Event.ScreenResumed -> loadLocalProgress()
        }
    }

    private fun loadLocalProgress() {
        viewModelScope.launch {
            val lookup = getMeaningfulVideoProgress()
                .associateBy { it.historyProgressKey }
                .toImmutableMap()
            setState { copy(localProgress = lookup) }
        }
    }
}
