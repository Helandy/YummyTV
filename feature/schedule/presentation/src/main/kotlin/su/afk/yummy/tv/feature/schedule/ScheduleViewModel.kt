package su.afk.yummy.tv.feature.schedule

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.contentLanguageChanges
import su.afk.yummy.tv.core.utils.coroutines.runSuspendCatching
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.schedule.mapper.toTimelineUi
import su.afk.yummy.tv.feature.schedule.mapper.withSelectedDay
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val getSchedule: GetAnimeScheduleUseCase,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val analytics: ScheduleAnalytics,
    private val accountSettingsStore: YaniAccountSettingsStore,
) : BaseViewModel<ScheduleState.State, ScheduleState.Event, ScheduleState.Effect>() {

    override fun createInitialState() = ScheduleState.State()

    init {
        analytics.eventScreenOpened()
        load()
        accountSettingsStore.contentLanguageChanges()
            .onEach { load() }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: ScheduleState.Event) {
        when (event) {
            ScheduleState.Event.BackSelected -> nav.back()

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

            ScheduleState.Event.RetrySelected -> {
                analytics.eventRetry()
                load()
            }
        }
    }

    private var loadJob: Job? = null

    private fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isLoading = true, error = null) }
            runSuspendCatching { getSchedule() }.fold(
                onSuccess = { days ->
                    setState {
                        copy(
                            isLoading = false,
                            days = days.toImmutableList(),
                            tvSchedule = days.toTimelineUi(
                                selectedEpochDay = tvSchedule.selectedEpochDay,
                            ),
                        )
                    }
                },
                onFailure = {
                    analytics.eventLoadError(it)
                    setState {
                        copy(
                            isLoading = false,
                            error = errorHandler.parse(it).message
                        )
                    }
                },
            )
        }
    }
}
