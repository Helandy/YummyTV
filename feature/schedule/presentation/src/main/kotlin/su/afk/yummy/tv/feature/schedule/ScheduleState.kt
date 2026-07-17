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

    /** Пользовательские действия на экране расписания. */
    sealed interface Event : UiEvent {
        /** Пользователь вернулся на предыдущий экран. */
        data object BackSelected : Event

        /** Пользователь запросил повторную загрузку расписания. */
        data object RetrySelected : Event

        /** Пользователь выбрал дату по значению epoch day. */
        data class DateSelected(val epochDay: Long) : Event

        /** Пользователь выбрал аниме с указанным идентификатором. */
        data class AnimeSelected(val animeId: Int) : Event
    }

    sealed interface Effect : UiEffect
}
