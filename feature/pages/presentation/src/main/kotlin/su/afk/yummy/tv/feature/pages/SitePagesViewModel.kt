package su.afk.yummy.tv.feature.pages

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.domain.pages.usecase.GetSitePageUseCase
import javax.inject.Inject

@HiltViewModel
class SitePagesViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val getSitePage: GetSitePageUseCase,
) : BaseViewModel<SitePagesState.State, SitePagesState.Event, SitePagesState.Effect>() {
    override fun createInitialState() = SitePagesState.State()

    override fun onEvent(event: SitePagesState.Event) {
        when (event) {
            SitePagesState.Event.BackSelected -> {
                if (currentState.selectedType == null) nav.back()
                else setState { SitePagesState.State() }
            }

            is SitePagesState.Event.PageSelected -> load(event.type)
            SitePagesState.Event.RetrySelected -> currentState.selectedType?.let(::load)
        }
    }

    private fun load(type: SitePageType) {
        viewModelScope.launch {
            setState {
                copy(
                    selectedType = type,
                    page = null,
                    loading = true,
                    usingFallback = false
                )
            }
            runSuspendCatching { getSitePage(type) }.fold(
                onSuccess = { page -> setState { copy(page = page, loading = false) } },
                onFailure = { setState { copy(loading = false, usingFallback = true) } },
            )
        }
    }
}
