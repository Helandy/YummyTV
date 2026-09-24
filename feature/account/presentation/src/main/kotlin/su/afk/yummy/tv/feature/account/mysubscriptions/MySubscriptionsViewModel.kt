package su.afk.yummy.tv.feature.account.mysubscriptions

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.account.usecase.GetVideoSubscriptionsUseCase
import su.afk.yummy.tv.feature.account.account.model.AccountUiError
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

/**
 * Список подписок пользователя.
 *
 * `/users/{id}/lists/subs` — единственный способ получить их все одним запросом, но озвучку он не
 * сообщает (см. docs/subscriptions.md), поэтому в строке показывается только тайтл и балансер.
 */
@HiltViewModel
class MySubscriptionsViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getAccountSession: GetAccountSessionUseCase,
    private val getVideoSubscriptions: GetVideoSubscriptionsUseCase,
) : BaseViewModel<MySubscriptionsState.State, MySubscriptionsState.Event, MySubscriptionsState.Effect>() {

    override fun createInitialState() = MySubscriptionsState.State()

    override fun onEvent(event: MySubscriptionsState.Event) {
        when (event) {
            MySubscriptionsState.Event.ScreenShown -> viewModelScope.launch {
                // Возврат из тайтла: подписку могли снять там, поэтому список перечитываем молча,
                // не показывая лоадер поверх уже показанных строк.
                load(showLoading = currentState.subscriptions.isEmpty())
            }

            MySubscriptionsState.Event.BackSelected -> nav.back()
            MySubscriptionsState.Event.RetrySelected -> viewModelScope.launch { load() }
            is MySubscriptionsState.Event.SubscriptionSelected ->
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
        }
    }

    private suspend fun load(showLoading: Boolean = true) {
        setState { copy(isLoading = showLoading, error = null) }
        val session = getAccountSession()
        if (!session.isAuthorized || session.userId <= 0) {
            setState {
                copy(
                    isLoading = false,
                    isSignedIn = false,
                    subscriptions = persistentListOf(),
                )
            }
            return
        }
        runSuspendCatching { getVideoSubscriptions(session.userId) }.fold(
            onSuccess = { subscriptions ->
                setState {
                    copy(
                        isLoading = false,
                        isSignedIn = true,
                        error = null,
                        subscriptions = subscriptions.toImmutableList(),
                    )
                }
            },
            onFailure = {
                setState {
                    copy(
                        isLoading = false,
                        isSignedIn = true,
                        error = AccountUiError.LOAD_SUBSCRIPTIONS_FAILED,
                        subscriptions = persistentListOf(),
                    )
                }
            },
        )
    }
}
