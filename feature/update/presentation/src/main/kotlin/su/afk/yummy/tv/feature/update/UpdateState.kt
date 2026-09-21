package su.afk.yummy.tv.feature.update

import su.afk.yummy.tv.core.mvi.UiEffect
import su.afk.yummy.tv.core.mvi.UiEvent
import su.afk.yummy.tv.core.mvi.UiState

class UpdateState {

    data class State(
        val status: Status = Status.Idle,
    ) : UiState {

        sealed class Status {
            data object Idle : Status()
            data class Available(
                val version: String,
                val changelog: String,
                val apkUrl: String,
                val required: Boolean = false,
                val updatesCount: Int = 0,
                val isPrerelease: Boolean = false,
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
        data class Init(
            val version: String,
            val apkUrl: String,
            val changelog: String,
            val required: Boolean = false,
            val updatesCount: Int = 0,
            val isPrerelease: Boolean = false,
        ) : Event()
        data object Dismiss : Event()
        data class ConfirmUpdate(val apkUrl: String) : Event()
        data class RetryUpdate(val apkUrl: String) : Event()
    }

    sealed class Effect : UiEffect
}
