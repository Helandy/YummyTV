package su.afk.yummy.tv.feature.schedule

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.BaseViewModelNew
import su.afk.yummy.tv.core.error.IErrorHandlerUseCase
import su.afk.yummy.tv.core.error.storage.RetryStorage
import su.afk.yummy.tv.core.navigation.NavigationManager
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val getSchedule: GetAnimeScheduleUseCase,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
) : BaseViewModelNew<ScheduleState.State, ScheduleState.Event, ScheduleState.Effect>(savedStateHandle) {

    override fun createInitialState() = ScheduleState.State()

    init {
        load()
    }

    override fun onEvent(event: ScheduleState.Event) {
        when (event) {
            is ScheduleState.Event.AnimeSelected -> nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            ScheduleState.Event.RetrySelected -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getSchedule() }.fold(
                onSuccess = { setState { copy(isLoading = false, days = it) } },
                onFailure = { setState { copy(isLoading = false, error = it.message ?: "Could not load schedule") } },
            )
        }
    }
}
