package su.afk.yummy.tv.feature.details.collections

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.account.usecase.GetAnimeCollectionsUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.details.DetailsAnalytics

@HiltViewModel(assistedFactory = CollectionsViewModel.Factory::class)
class CollectionsViewModel @AssistedInject internal constructor(
    @Assisted private val animeId: Int,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val collectionNavigator: ICollectionNavigator,
    private val getAnimeCollections: GetAnimeCollectionsUseCase,
    private val analytics: DetailsAnalytics,
) : BaseViewModel<CollectionsState.State, CollectionsState.Event, CollectionsState.Effect>() {

    @AssistedFactory
    interface Factory {
        fun create(animeId: Int): CollectionsViewModel
    }

    override fun createInitialState() = CollectionsState.State()

    init {
        analytics.eventCollectionsScreenOpened(animeId)
        viewModelScope.launch { load() }
    }

    override fun onEvent(event: CollectionsState.Event) {
        when (event) {
            CollectionsState.Event.BackSelected -> nav.back()
            CollectionsState.Event.RetrySelected -> {
                analytics.eventCollectionsRetry(animeId)
                viewModelScope.launch { load() }
            }

            is CollectionsState.Event.CollectionSelected -> {
                analytics.eventCollectionsCollectionSelected(animeId, event.collectionId)
                nav.navigate(collectionNavigator.getCollectionDest(event.collectionId))
            }
        }
    }

    private suspend fun load() {
        setState { copy(isLoading = true, error = null) }
        runSuspendCatching { getAnimeCollections(animeId) }.fold(
            onSuccess = { collections ->
                setState {
                    copy(
                        isLoading = false,
                        collections = collections.distinctBy { it.id }.toImmutableList(),
                        error = null
                    )
                }
            },
            onFailure = { e ->
                analytics.eventCollectionsLoadError(e)
                setState {
                    copy(
                        isLoading = false,
                        error = e.userMessage(),
                        collections = persistentListOf()
                    )
                }
            },
        )
    }

}
