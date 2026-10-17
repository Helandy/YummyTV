package su.afk.yummy.tv.feature.details.sitelink

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.domain.account.usecase.ResolveNotificationAnimeIdUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.navigator.DetailsSiteLinkDestination

/**
 * Открывает тайтл по ссылке сайта: slug → id тем же запросом, что и уведомления, затем заменяет
 * себя экраном деталей.
 */
@HiltViewModel(assistedFactory = DetailsSiteLinkViewModel.Factory::class)
class DetailsSiteLinkViewModel @AssistedInject internal constructor(
    @Assisted private val slug: String,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val resolveAnimeId: ResolveNotificationAnimeIdUseCase,
) : BaseViewModel<DetailsSiteLinkState.State, DetailsSiteLinkState.Event, DetailsSiteLinkState.Effect>() {

    override fun createInitialState() = DetailsSiteLinkState.State()

    init {
        resolve()
    }

    override fun onEvent(event: DetailsSiteLinkState.Event) {
        when (event) {
            DetailsSiteLinkState.Event.RetrySelected -> resolve()
            DetailsSiteLinkState.Event.BackSelected -> nav.back()
        }
    }

    private fun resolve() {
        setState { copy(isFailed = false) }
        viewModelScope.launch {
            val animeId = runSuspendCatching { resolveAnimeId(slug) }.getOrNull()
            // Пока шёл запрос, пользователь мог уйти назад — заменяем только свой экран.
            if (nav.backStack.lastOrNull() != DetailsSiteLinkDestination(slug)) return@launch
            if (animeId == null) {
                setState { copy(isFailed = true) }
            } else {
                nav.replace(detailsNavigator.getDetailsDest(animeId))
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(slug: String): DetailsSiteLinkViewModel
    }
}
