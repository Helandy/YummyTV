package su.afk.yummy.tv.feature.details.subscriptions

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.details.handler.DetailsSubscriptionHandler
import su.afk.yummy.tv.feature.details.details.handler.ScreenSubscriptionBaseResult

@HiltViewModel(assistedFactory = SubscriptionsViewModel.Factory::class)
class SubscriptionsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val subscriptionHandler: DetailsSubscriptionHandler,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<SubscriptionsState.State, SubscriptionsState.Event, SubscriptionsState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): SubscriptionsViewModel
    }

    override fun createInitialState() = SubscriptionsState.State()

    init {
        analytics.eventSubscriptionsScreenOpened(animeId)
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: SubscriptionsState.Event) {
        when (event) {
            SubscriptionsState.Event.BackSelected -> nav.back()
            SubscriptionsState.Event.RetrySelected -> {
                analytics.eventSubscriptionsRetry(animeId)
                viewModelScope.launch { load() }
            }

            is SubscriptionsState.Event.SubscriptionToggled -> toggleSubscription(event.key)
        }
    }

    private suspend fun load(showLoading: Boolean = true) {
        if (showLoading) {
            setState { copy(isLoading = true, error = null) }
        }

        when (val result = subscriptionHandler.loadScreenSubscriptionBase(animeId)) {
            ScreenSubscriptionBaseResult.SignedOut -> {
                setState { copy(isLoading = false, subscriptions = persistentListOf()) }
            }

            is ScreenSubscriptionBaseResult.Content -> {
                setState {
                    copy(
                        isLoading = false,
                        error = null,
                        subscriptions = result.base.subscriptions.toImmutableList(),
                    )
                }
            }

            is ScreenSubscriptionBaseResult.Failure -> {
                analytics.eventSubscriptionsLoadError(result.error)
                setState {
                    copy(
                        isLoading = false,
                        error = UiText.Failure(result.error),
                        subscriptions = persistentListOf(),
                    )
                }
            }
        }
    }

    private fun toggleSubscription(key: String) {
        val option = currentState.subscriptions.firstOrNull { it.key == key } ?: return
        val wasSubscribed = option.isSubscribed
        analytics.eventSubscriptionsSubscriptionToggled(
            animeId = animeId,
            videoId = option.subscriptionVideoId,
            targetState = !wasSubscribed,
        )
        setSubscriptionState(key, !wasSubscribed)
        viewModelScope.launch {
            val changed = subscriptionHandler.commitSubscriptionChange(
                animeId = animeId,
                option = option,
                subscribed = !wasSubscribed,
            )
            if (!changed) {
                setSubscriptionState(key, wasSubscribed)
                return@launch
            }
            subscriptionHandler.reloadSubscriptions(animeId).onSuccess { subscriptions ->
                setState { copy(subscriptions = subscriptions.toImmutableList()) }
            }
        }
    }

    private fun setSubscriptionState(key: String, subscribed: Boolean) {
        setState {
            copy(
                subscriptions = subscriptions.map {
                    if (it.key == key) it.copy(isSubscribed = subscribed) else it
                }.toImmutableList(),
            )
        }
    }

}
