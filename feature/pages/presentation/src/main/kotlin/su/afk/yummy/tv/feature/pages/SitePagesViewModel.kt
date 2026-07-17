package su.afk.yummy.tv.feature.pages

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.pages.model.SitePageType
import su.afk.yummy.tv.domain.pages.usecase.GetSitePageUseCase
import javax.inject.Inject

@HiltViewModel
class SitePagesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val nav: NavigationManager,
    private val getSitePage: GetSitePageUseCase,
) : BaseViewModelNew<SitePagesState.State, SitePagesState.Event, SitePagesState.Effect>(
    savedStateHandle
) {
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
            runCatching { getSitePage(type) }.fold(
                onSuccess = { page -> setState { copy(page = page, loading = false) } },
                onFailure = { setState { copy(loading = false, usingFallback = true) } },
            )
        }
    }
}
