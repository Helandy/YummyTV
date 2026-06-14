package su.afk.yummy.tv.feature.details.collections

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
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator

@HiltViewModel(assistedFactory = CollectionsViewModel.Factory::class)
class CollectionsViewModel @AssistedInject constructor(
    @Assisted private val animeId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val collectionNavigator: ICollectionNavigator,
    private val getAnimeCollections: GetAnimeCollectionsUseCase,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<CollectionsState.State, CollectionsState.Event, CollectionsState.Effect>(
    savedStateHandle
) {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): CollectionsViewModel
    }

    override fun createInitialState() = CollectionsState.State()

    init {
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: CollectionsState.Event) {
        when (event) {
            CollectionsState.Event.BackSelected -> nav.back()
            CollectionsState.Event.RetrySelected -> {
                trackCollectionsAction("retry")
                viewModelScope.launch { load() }
            }

            is CollectionsState.Event.CollectionSelected -> {
                trackCollectionsAction(
                    action = "collection_selected",
                    params = analyticsParamsOf("collection_id" to event.collectionId),
                )
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runCatching { getAnimeCollections(animeId) }.fold(
            onSuccess = { collections ->
                setState { copy(isLoading = false, collections = collections, error = null) }
            },
            onFailure = { e ->
                setState { copy(isLoading = false, error = e.message, collections = emptyList()) }
            },
        )
    }

    private fun trackCollectionsAction(
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

private const val SCREEN_NAME = "details_collections"
