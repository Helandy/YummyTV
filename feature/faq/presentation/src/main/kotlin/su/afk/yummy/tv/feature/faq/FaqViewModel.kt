package su.afk.yummy.tv.feature.faq

import dagger.hilt.android.lifecycle.HiltViewModel
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import javax.inject.Inject

@HiltViewModel
class FaqViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
) : BaseViewModel<FaqState.State, FaqState.Event, FaqState.Effect>() {
    override fun createInitialState() = FaqState.State

    override fun onEvent(event: FaqState.Event) {
        when (event) {
            FaqState.Event.BackSelected -> nav.back()
        }
    }
}
