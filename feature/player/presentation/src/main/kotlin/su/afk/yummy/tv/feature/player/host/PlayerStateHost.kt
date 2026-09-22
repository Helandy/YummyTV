package su.afk.yummy.tv.feature.player.host

import kotlinx.coroutines.CoroutineScope
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerStreamResumeMode

/**
 * Узкий доступ делегатов плеера к состоянию экрана.
 *
 * Делегаты не видят `BaseViewModel` целиком: только текущее состояние, reducer и scope,
 * живущий столько же, сколько ViewModel.
 */
internal interface PlayerStateHost {
    val state: PlayerState.State
    val scope: CoroutineScope
    fun update(reducer: PlayerState.State.() -> PlayerState.State)
}

/** Хост для поведений источника: плюс управление загрузкой потока, которой владеет ViewModel. */
internal interface PlayerSourceHost : PlayerStateHost {
    val changePlayerHint: PlayerChangePlayerHint

    /** Запускает получение потока для активного источника, отменяя предыдущее. */
    fun loadStream(request: PlayerStreamLoadRequest = PlayerStreamLoadRequest())

    /** Отменяет идущее получение потока. */
    fun cancelStreamLoad()

    /** Закрывает сессии всех источников (сейчас это живая сессия Alloha). */
    fun closeSourceSessions()

    /** Текст общей ошибки потока для оверлея «повторить/сменить». */
    fun streamErrorMessage(): String
}

/** Параметры [PlayerSourceHost.loadStream]; значения по умолчанию — обычная загрузка. */
internal data class PlayerStreamLoadRequest(
    val resumeMode: PlayerStreamResumeMode = PlayerStreamResumeMode.PreserveCurrent,
    val refreshSourcesOnFailure: Boolean = true,
    val forceFreshAllohaSession: Boolean = false,
    val selectedQualityOverride: String? = null,
    val forceRefresh: Boolean = false,
)
