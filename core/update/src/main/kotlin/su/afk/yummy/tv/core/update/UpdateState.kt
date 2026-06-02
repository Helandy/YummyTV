package su.afk.yummy.tv.core.update

import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEffect
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiEvent
import su.afk.yummy.tv.core.designsystem.presenter.baseViewModel.UiState

object UpdateState {

    data class State(
        val status: Status = Status.Idle,
    ) : UiState {

        sealed class Status {
            data object Idle : Status()
            data class Available(
                val version: String,
                val changelog: String,
                val apkUrl: String,
                val installSupported: Boolean,
            ) : Status()
            data class Downloading(val progress: Float) : Status()
            data object Installing : Status()
            data class Error(
                val message: String,
                val apkUrl: String? = null,
            ) : Status()
        }
    }

    sealed class Event : UiEvent {
        data object Dismiss : Event()
        data class ConfirmUpdate(val apkUrl: String) : Event()
        data class RetryUpdate(val apkUrl: String) : Event()
    }

    sealed class Effect : UiEffect {
        data object NavigateToUpdate : Effect()
        data object NavigateBack : Effect()
    }
}
