package su.afk.yummy.tv.feature.schedule

import androidx.lifecycle.SavedStateHandle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.analytics.AnalyticsEvents
import su.afk.yummy.tv.core.analytics.AnalyticsTracker
import su.afk.yummy.tv.core.analytics.analyticsParamsOf
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
class ScheduleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    override val errorHandler: IErrorHandlerUseCase,
    override val retryStorage: RetryStorage,
    private val getSchedule: GetAnimeScheduleUseCase,
    private val nav: NavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val analyticsTracker: AnalyticsTracker,
) : BaseViewModelNew<ScheduleState.State, ScheduleState.Event, ScheduleState.Effect>(
    savedStateHandle
) {

    override fun createInitialState() = ScheduleState.State()

    init {
        load()
    }

    override fun onEvent(event: ScheduleState.Event) {
        when (event) {
            is ScheduleState.Event.AnimeSelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "anime_selected",
                        params = analyticsParamsOf("anime_id" to event.animeId),
                    )
                )
                nav.navigate(detailsNavigator.getDetailsDest(event.animeId))
            }

            is ScheduleState.Event.DateSelected -> {
                analyticsTracker.track(
                    AnalyticsEvents.uiAction(
                        screenName = SCREEN_NAME,
                        action = "date_selected",
                        params = analyticsParamsOf("epoch_day" to event.epochDay),
                    )
                )
                setState {
                    copy(tvSchedule = tvSchedule.withSelectedDay(event.epochDay))
                }
            }

            is ScheduleState.Event.ReleaseFocused -> setState {
                copy(tvSchedule = tvSchedule.withFocusedRelease(event.releaseKey, event.epochDay))
            }

            ScheduleState.Event.RetrySelected -> {
                analyticsTracker.track(AnalyticsEvents.uiAction(SCREEN_NAME, "retry"))
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

private const val SCREEN_NAME = "schedule"
