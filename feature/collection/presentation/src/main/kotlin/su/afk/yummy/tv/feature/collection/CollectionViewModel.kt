package su.afk.yummy.tv.feature.collection

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
import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionUseCase
import su.afk.yummy.tv.feature.collection.presentation.R
import su.afk.yummy.tv.feature.details.IDetailsNavigator

@HiltViewModel(assistedFactory = CollectionViewModel.Factory::class)
class CollectionViewModel @AssistedInject internal constructor(
    @Assisted private val collectionId: Int,
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val getCollection: GetCollectionUseCase,
    private val stringProvider: StringProvider,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<CollectionState.State, CollectionState.Event, CollectionState.Effect>(savedStateHandle) {

    @AssistedFactory
    interface Factory {
        fun create(collectionId: Int): CollectionViewModel
    }

    override fun createInitialState() = CollectionState.State()

    init {
        load()
    }

    override fun onEvent(event: CollectionState.Event) {
        when (event) {
            CollectionState.Event.BackSelected -> nav.back()
            CollectionState.Event.RetrySelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "retry",
                        params = analyticsParamsOf("collection_id" to collectionId),
                    )
                )
                load()
            }
            is CollectionState.Event.AnimeSelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "anime_selected",
                        params = analyticsParamsOf(
                            "collection_id" to collectionId,
                            "anime_id" to event.animeId,
                        ),
                    )
                )
                setState {
                    copy(
                        focusedItemId = event.animeId,
                        restoreFocusedItemOnEnter = true,
                    )
                }
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }
            is CollectionState.Event.ItemFocused -> onItemFocused(event.animeId)
            is CollectionState.Event.GridScrolled -> setState {
                copy(firstVisibleItemIndex = event.index, firstVisibleItemScrollOffset = event.offset)
            }
            CollectionState.Event.FocusedItemRestoreHandled -> {
                if (currentState.restoreFocusedItemOnEnter) {
                    setState { copy(restoreFocusedItemOnEnter = false) }
                }
            }
        }
    }

    private fun onItemFocused(animeId: Int) {
        if (currentState.focusedItemId == animeId) return
        setState { copy(focusedItemId = animeId) }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getCollection(collectionId) }.fold(
                onSuccess = { collection -> setState { copy(isLoading = false, collection = collection) } },
                onFailure = { e ->
                    setState { copy(isLoading = false, error = e.message ?: stringProvider.get(R.string.collection_load_error)) }
                },
            )
        }
    }
}

private const val SCREEN_NAME = "collection"
