package su.afk.yummy.tv.feature.commonscreen.errorScreen

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.feature.commonscreen.CommonScreenAnalytics
import su.afk.yummy.tv.feature.commonscreen.navigator.CommonScreenDestination

@HiltViewModel(assistedFactory = ErrorViewModel.Factory::class)
internal class ErrorViewModel @AssistedInject constructor(
    @Assisted private val dest: CommonScreenDestination.ErrorNavigatorDest,
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val navManager: INavigationManager,
    private val analytics: CommonScreenAnalytics,
) : BaseViewModel<ErrorScreenState.State, ErrorScreenState.Event, ErrorScreenState.Effect>() {

    override fun createInitialState(): ErrorScreenState.State = ErrorScreenState.State()

    @AssistedFactory
    interface Factory {
        fun create(
            dest: CommonScreenDestination.ErrorNavigatorDest,
        ): ErrorViewModel
    }

    init {
        analytics.eventErrorShown(dest.analyticsParams)
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
