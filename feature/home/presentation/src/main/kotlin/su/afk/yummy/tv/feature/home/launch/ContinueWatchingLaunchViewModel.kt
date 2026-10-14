package su.afk.yummy.tv.feature.home.launch

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.home.usecase.ObserveContinueWatchingUseCase
import su.afk.yummy.tv.domain.watching.usecase.ResolveContinueWatchingLaunchUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.home.navigator.ContinueWatchingLaunchDestination
import su.afk.yummy.tv.feature.home.utils.hasPlayableTarget
import su.afk.yummy.tv.feature.player.IPlayerNavigator
import su.afk.yummy.tv.feature.player.getPlayerDest

/**
 * Открывает плеер по записи «продолжить просмотр» тайтла из ярлыка или виджета — так же, как
 * карточка на главной. Нет записи, цели для запуска или запуск не разрешился — детали тайтла.
 * Тост о переключении на серверный прогресс здесь не показываем: экран сразу заменяется плеером.
 */
@HiltViewModel(assistedFactory = ContinueWatchingLaunchViewModel.Factory::class)
class ContinueWatchingLaunchViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val playerNavigator: IPlayerNavigator,
    private val observeContinueWatching: ObserveContinueWatchingUseCase,
    private val resolveContinueWatchingLaunch: ResolveContinueWatchingLaunchUseCase,
    private val settingsStore: SettingsStore,
) : BaseViewModel<ContinueWatchingLaunchState.State, ContinueWatchingLaunchState.Event, ContinueWatchingLaunchState.Effect>() {

    override fun createInitialState() = ContinueWatchingLaunchState.State

    init {
        viewModelScope.launch { launch() }
    }

    override fun onEvent(event: ContinueWatchingLaunchState.Event) = Unit

    private suspend fun launch() {
        val destination = runSuspendCatching {
            val entry = observeContinueWatching().first()
                .firstOrNull { it.animeId == animeId }
                ?.takeIf { it.hasPlayableTarget() }
                ?: return@runSuspendCatching null
            val launch = resolveContinueWatchingLaunch(
                entry = entry,
                refreshProgressOnLaunch = settingsStore.refreshContinueWatchingProgressOnLaunch.first(),
            )
            playerNavigator.getPlayerDest(launch)
        }.getOrNull()
        // Пока шёл запуск, пользователь мог уйти назад или открыть другой ярлык: заменяем только
        // свой экран, иначе плеер встал бы поверх чужого ключа.
        if (nav.backStack.lastOrNull() != ContinueWatchingLaunchDestination(animeId)) return
        nav.replace(destination ?: detailsNavigator.getDetailsDest(animeId))
    }

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): ContinueWatchingLaunchViewModel
    }
}
