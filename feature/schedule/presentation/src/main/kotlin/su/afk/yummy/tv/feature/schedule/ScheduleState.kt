package su.afk.yummy.tv.feature.schedule

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.feature.schedule.model.ScheduleTimelineUi

class ScheduleState {
    data class State(
        val isLoading: Boolean = true,
        val days: List<AnimeScheduleDay> = emptyList(),
        val tvSchedule: ScheduleTimelineUi = ScheduleTimelineUi(),
        val error: String? = null,
    ) : UiState

    sealed interface Event : UiEvent {
        data object RetrySelected : Event
        data class DateSelected(val epochDay: Long) : Event
        data class ReleaseFocused(val releaseKey: String, val epochDay: Long) : Event
        data class AnimeSelected(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
