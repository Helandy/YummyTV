package su.afk.yummy.tv.feature.commonscreen.errorScreen

import androidx.lifecycle.SavedStateHandle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.feature.commonscreen.CommonScreenAnalytics
import su.afk.yummy.tv.feature.commonscreen.navigator.CommonScreenDestination

internal class ErrorViewModel @AssistedInject constructor(
    @Assisted private val dest: CommonScreenDestination.ErrorNavigatorDest,
    @Assisted savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val navManager: NavigationManager,
    private val analytics: CommonScreenAnalytics,
) : BaseViewModelNew<ErrorScreenState.State, ErrorScreenState.Event, ErrorScreenState.Effect>(savedStateHandle) {

    override fun createInitialState(): ErrorScreenState.State = ErrorScreenState.State()

    @AssistedFactory
    interface Factory {
        fun create(
            dest: CommonScreenDestination.ErrorNavigatorDest,
            savedStateHandle: SavedStateHandle
        ): ErrorViewModel
    }

    init {
        analytics.eventErrorShown(dest.screenParams)
        setState { copy(error = dest.error) }
    }

    override fun onEvent(event: ErrorScreenState.Event) {
        when (event) {
            ErrorScreenState.Event.Retry -> retry()
            ErrorScreenState.Event.Back -> back()
        }
    }

    private fun retry() {
        analytics.eventErrorRetry()
        val key = currentState.error?.retryKey ?: return
        retryStorage.consume(key)?.invoke()
        navManager.back()
    }

    private fun back() {
        analytics.eventErrorBack()
        navManager.backTwo()
    }

    override fun onCleared() {
        dest.error.retryKey?.let(retryStorage::remove)
        super.onCleared()
    }
}
