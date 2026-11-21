package su.afk.yummy.tv.feature.schedule

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.preferences.settings.contentLanguageChanges
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryAnimeIdsUseCase
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.usecase.GetAnimeScheduleUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.schedule.mapper.toTimelineUi
import su.afk.yummy.tv.feature.schedule.mapper.withSelectedDay
import su.afk.yummy.tv.feature.schedule.model.ScheduleTimelineUi
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject internal constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val getSchedule: GetAnimeScheduleUseCase,
    private val observeLibraryAnimeIds: ObserveLibraryAnimeIdsUseCase,
    private val nav: INavigationManager,
    private val detailsNavigator: IDetailsNavigator,
    private val analytics: ScheduleAnalytics,
    private val accountSettingsStore: YaniAccountSettingsStore,
    private val appClock: AppClock,
) : BaseViewModel<ScheduleState.State, ScheduleState.Event, ScheduleState.Effect>() {

    override fun createInitialState() = ScheduleState.State()

    private var loadJob: Job? = null
    private var libraryAnimeIds: Set<Int> = emptySet()

    init {
        analytics.eventScreenOpened()
        observeLibraryAnimeIds()
            .onEach { ids ->
                libraryAnimeIds = ids
                if (currentState.onlyInLibrary && currentState.days.isNotEmpty()) {
                    setState {
                        copy(
                            tvSchedule = days.buildTimeline(
                                selectedEpochDay = tvSchedule.selectedEpochDay,
                                onlyInLibrary = true,
                            ),
                        )
                    }
                }
            }
            .launchIn(viewModelScope)
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

            ScheduleState.Event.InLibraryFilterToggled -> {
                val enabled = !currentState.onlyInLibrary
                analytics.eventLibraryFilterToggled(enabled)
                setState {
                    copy(
                        onlyInLibrary = enabled,
                        tvSchedule = days.buildTimeline(
                            selectedEpochDay = tvSchedule.selectedEpochDay,
                            onlyInLibrary = enabled,
                        ),
                    )
                }
            }

            ScheduleState.Event.RetrySelected -> {
                analytics.eventRetry()
                load()
            }

            ScheduleState.Event.RefreshRequested -> refresh()
        }
    }

    /** Перезагружает расписание, не пряча уже показанные дни; ошибку при них не показывает. */
    private fun refresh() {
        if (currentState.days.isEmpty()) return load()
        if (currentState.isRefreshing) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            setState { copy(isRefreshing = true) }
            runSuspendCatching { getSchedule() }.fold(
                onSuccess = { days ->
                    setState {
                        copy(
                            isRefreshing = false,
                            days = days.toImmutableList(),
                            tvSchedule = days.buildTimeline(
                                selectedEpochDay = tvSchedule.selectedEpochDay,
                                onlyInLibrary = onlyInLibrary,
                            ),
                        )
                    }
                },
                onFailure = {
                    analytics.eventLoadError(it)
                    setState { copy(isRefreshing = false) }
                },
            )
        }
    }

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
                            tvSchedule = days.buildTimeline(
                                selectedEpochDay = tvSchedule.selectedEpochDay,
                                onlyInLibrary = onlyInLibrary,
                            ),
                        )
                    }
                },
                onFailure = {
                    analytics.eventLoadError(it)
                    setState {
                        copy(
                            isLoading = false,
                            error = UiText.Failure(it),
                        )
                    }
                },
            )
        }
    }

    private fun List<AnimeScheduleDay>.buildTimeline(
        selectedEpochDay: Long?,
        onlyInLibrary: Boolean,
    ): ScheduleTimelineUi =
        toTimelineUi(
            now = Instant.ofEpochMilli(appClock.nowMillis()).atZone(ZoneId.systemDefault()),
            selectedEpochDay = selectedEpochDay,
            allowedAnimeIds = libraryAnimeIds.takeIf { onlyInLibrary },
        )
}
