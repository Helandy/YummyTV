package su.afk.yummy.tv.feature.details.subscriptions

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.details.details.handler.DetailsSubscriptionHandler
import su.afk.yummy.tv.feature.details.details.handler.ScreenSubscriptionBaseResult
import su.afk.yummy.tv.feature.details.utils.subscribedKeys

@HiltViewModel(assistedFactory = SubscriptionsViewModel.Factory::class)
class SubscriptionsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val subscriptionHandler: DetailsSubscriptionHandler,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<SubscriptionsState.State, SubscriptionsState.Event, SubscriptionsState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): SubscriptionsViewModel
    }

    override fun createInitialState() = SubscriptionsState.State()

    init {
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: SubscriptionsState.Event) {
        when (event) {
            SubscriptionsState.Event.BackSelected -> nav.back()
            SubscriptionsState.Event.RetrySelected -> {
                trackSubscriptionsAction("retry")
                viewModelScope.launch { load() }
            }
            is SubscriptionsState.Event.SubscriptionToggled -> toggleSubscription(event.key)
        }
    }

    private suspend fun load(showLoading: Boolean = true) {
        if (showLoading) {
            setState { copy(isLoading = true, error = null) }
        }

        when (val result = subscriptionHandler.loadScreenSubscriptionBase(
            animeId = animeId,
            optimisticKeys = currentState.subscriptions.subscribedKeys(),
        )) {
            ScreenSubscriptionBaseResult.SignedOut -> {
                setState { copy(isLoading = false, subscriptions = emptyList()) }
            }

            is ScreenSubscriptionBaseResult.Content -> {
                val base = result.base
                setState { copy(subscriptions = base.subscriptions) }
                subscriptionHandler.loadDetailsSubscriptions(
                    animeId = animeId,
                    details = base.details,
                    videos = base.videos,
                    userId = base.userId,
                    optimisticKeys = currentState.subscriptions.subscribedKeys(),
                ).fold(
                    onSuccess = { subscriptions ->
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                subscriptions = subscriptions,
                            )
                        }
                    },
                    onFailure = {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                subscriptions = base.subscriptions,
                            )
                        }
                    },
                )
            }

            is ScreenSubscriptionBaseResult.Failure -> {
                setState {
                    copy(
                        isLoading = false,
                        error = result.message,
                        subscriptions = emptyList(),
                    )
                }
            }
        }
    }

    private fun toggleSubscription(key: String) {
        val option = currentState.subscriptions.firstOrNull { it.key == key } ?: return
        val wasSubscribed = option.isSubscribed
        trackSubscriptionsAction(
            action = "subscription_toggled",
            params = analyticsParamsOf(
                "video_id" to option.representativeVideoId,
                "target_state" to (!wasSubscribed),
            ),
        )
        setSubscriptionState(key, !wasSubscribed)
        viewModelScope.launch {
            val changed = subscriptionHandler.commitSubscriptionChange(
                videoId = option.representativeVideoId,
                subscribed = !wasSubscribed,
            )
            if (!changed) {
                setSubscriptionState(key, wasSubscribed)
            } else {
                load(showLoading = false)
            }
        }
    }

    private fun setSubscriptionState(key: String, subscribed: Boolean) {
        setState {
            copy(
                subscriptions = subscriptions.map {
                    if (it.key == key) it.copy(isSubscribed = subscribed) else it
                },
            )
        }
    }

    private fun trackSubscriptionsAction(
        action: String,
        params: Map<String, String> = emptyMap(),
    ) {
        analyticsTracker.track(
            AnalyticsEvents.uiAction(
                screenName = SCREEN_NAME,
                action = action,
                params = analyticsParamsOf("anime_id" to animeId) + params,
            )
        )
    }
}

private const val SCREEN_NAME = "details_subscriptions"
