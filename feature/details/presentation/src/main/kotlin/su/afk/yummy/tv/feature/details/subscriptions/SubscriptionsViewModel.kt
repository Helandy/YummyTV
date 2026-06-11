package su.afk.yummy.tv.feature.details.subscriptions

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.core.preferences.auth.YaniAuthPreferences
import su.afk.yummy.tv.core.preferences.settings.SettingsStore
import su.afk.yummy.tv.domain.account.usecase.GetVideoSubscriptionsUseCase
import su.afk.yummy.tv.domain.account.usecase.SetVideoSubscriptionUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeDetailsUseCase
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeVideosUseCase
import su.afk.yummy.tv.feature.details.utils.SUBSCRIPTION_REFRESH_DELAY_MS
import su.afk.yummy.tv.feature.details.utils.matchesCurrentAnime
import su.afk.yummy.tv.feature.details.utils.subscribedKeys
import su.afk.yummy.tv.feature.details.utils.toSubscriptionOptions

@HiltViewModel(assistedFactory = SubscriptionsViewModel.Factory::class)
class SubscriptionsViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val yaniAuthPreferences: YaniAuthPreferences,
    private val settingsStore: SettingsStore,
    private val getAnimeDetails: GetAnimeDetailsUseCase,
    private val getAnimeVideos: GetAnimeVideosUseCase,
    private val getVideoSubscriptions: GetVideoSubscriptionsUseCase,
    private val setVideoSubscription: SetVideoSubscriptionUseCase,
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
            SubscriptionsState.Event.RetrySelected -> viewModelScope.launch { load() }
            is SubscriptionsState.Event.SubscriptionToggled -> toggleSubscription(event.key)
        }
    }

    private suspend fun load(showLoading: Boolean = true) {
        if (showLoading) {
            setState { copy(isLoading = true, error = null) }
        }

        val token = yaniAuthPreferences.refreshToken.first()
        val userId = settingsStore.yaniUserId.first()
        if (token.isBlank() || userId <= 0) {
            setState { copy(isLoading = false, subscriptions = emptyList()) }
            return
        }

        val details = runCatching { getAnimeDetails(animeId) }.getOrNull()
        runCatching { getAnimeVideos(animeId) }.fold(
            onSuccess = { videos ->
                val optimisticKeys = currentState.subscriptions.subscribedKeys()
                val baseOptions = videos.toSubscriptionOptions(optimisticKeys = optimisticKeys)
                setState { copy(subscriptions = baseOptions) }

                runCatching { getVideoSubscriptions(userId) }.fold(
                    onSuccess = { remoteSubscriptions ->
                        val animeSubscriptions = remoteSubscriptions
                            .filter {
                                it.matchesCurrentAnime(
                                    requestedAnimeId = animeId,
                                    details = details
                                )
                            }
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                subscriptions = videos.toSubscriptionOptions(
                                    remoteSubscriptions = animeSubscriptions,
                                    optimisticKeys = optimisticKeys,
                                ),
                            )
                        }
                    },
                    onFailure = {
                        setState {
                            copy(
                                isLoading = false,
                                error = null,
                                subscriptions = baseOptions
                            )
                        }
                    },
                )
            },
            onFailure = { error ->
                setState {
                    copy(
                        isLoading = false,
                        error = error.message,
                        subscriptions = emptyList(),
                    )
                }
            },
        )
    }

    private fun toggleSubscription(key: String) {
        val option = currentState.subscriptions.firstOrNull { it.key == key } ?: return
        val wasSubscribed = option.isSubscribed
        setSubscriptionState(key, !wasSubscribed)
        viewModelScope.launch {
            val result =
                runCatching { setVideoSubscription(option.representativeVideoId, !wasSubscribed) }
            if (result.isFailure) {
                setSubscriptionState(key, wasSubscribed)
            } else {
                delay(SUBSCRIPTION_REFRESH_DELAY_MS)
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
}
