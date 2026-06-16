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
import su.afk.yummy.tv.feature.schedule.utils.toTimelineUi
import su.afk.yummy.tv.feature.schedule.utils.withFocusedRelease
import su.afk.yummy.tv.feature.schedule.utils.withSelectedDay
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject internal constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val getSchedule: GetAnimeScheduleUseCase,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val analytics: ScheduleAnalytics,
) : BaseViewModelNew<ScheduleState.State, ScheduleState.Event, ScheduleState.Effect>(
    savedStateHandle
) {

    override fun createInitialState() = ScheduleState.State()

    init {
        analytics.eventScreenOpened()
        load()
    }

    override fun onEvent(event: ScheduleState.Event) {
        when (event) {
            is ScheduleState.Event.AnimeSelected -> {
                analytics.eventAnimeSelected(event.animeId)
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            is ScheduleState.Event.DateSelected -> {
                analytics.eventDateSelected(event.epochDay)
                setState {
                    copy(tvSchedule = tvSchedule.withSelectedDay(event.epochDay))
                }
            }

            is ScheduleState.Event.ReleaseFocused -> setState {
                copy(tvSchedule = tvSchedule.withFocusedRelease(event.releaseKey, event.epochDay))
            }

            ScheduleState.Event.RetrySelected -> {
                analytics.eventRetry()
                load()
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runCatching { getSchedule() }.fold(
                onSuccess = { days ->
                    setState {
                        copy(
                            isLoading = false,
                            days = days,
                            tvSchedule = days.toTimelineUi(
                                focusedReleaseKey = tvSchedule.focusedReleaseKey,
                                focusedReleaseEpochDay = tvSchedule.focusedReleaseEpochDay,
                            ),
                        )
                    }
                },
                onFailure = {
                    analytics.eventLoadError(it)
                    setState {
                        copy(
                            isLoading = false,
                            error = it.message ?: "Could not load schedule"
                        )
                    }
                },
            )
        }
    }
}
