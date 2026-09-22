package su.afk.yummy.tv.feature.player.behavior

import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.handler.PlayerStreamLoadResult
import su.afk.yummy.tv.feature.player.host.PlayerSourceHost

/**
 * Поведение плеера, зависящее от источника (балансера): как переживать ошибки воспроизведения
 * и что делать с результатом получения потока.
 *
 * Балансер меняется внутри одного экрана, поэтому ViewModel держит все поведения сразу:
 * - решения (ошибка, ретрай, успешный старт) принимает первое, чей [handles] подходит;
 * - хуки жизненного цикла ([reset], [close], хуки загрузки потока) получают все поведения:
 *   состояние старого источника должно закрыться и после смены балансера.
 */
internal interface PlayerSourceBehavior {

    fun attach(host: PlayerSourceHost)

    /** Отвечает ли поведение за ошибки и ретраи текущего источника. */
    fun handles(state: PlayerState.State): Boolean

    /** Идёт фоновое восстановление, при котором новые ошибки плеера — его же отголоски. */
    val isRecovering: Boolean get() = false

    /** Ошибка ExoPlayer. true — поведение запустило своё восстановление, false — показать ошибку. */
    fun onPlaybackError(event: PlayerState.Event.PlaybackError): Boolean

    /** Пользователь нажал «повторить». true — обработано, false — общий перезапрос потока. */
    fun onRetryRequested(): Boolean = false

    /** Плеер стартовал после восстановления, которое вело это поведение. */
    fun onPlaybackRecovered() {}

    /** Плеер успешно стартовал (получают все поведения). */
    fun onPlaybackReady() {}

    /** Позиция воспроизведения активного источника сдвинулась (получают все поведения). */
    fun onPlaybackPositionChanged(positionMs: Long) {}

    /** Держать ли текущий поток на экране, пока резолвится новый. */
    fun keepsStreamWhileResolving(): Boolean = false

    /** Позиция, с которой продолжить после восстановления, или null. */
    fun recoveryResumePositionMs(): Long? = null

    /** Поток успешно получен — можно активировать связанную с ним сессию. */
    fun onStreamActivated(result: PlayerStreamLoadResult.State) {}

    /** Резолв провалился. true — поведение само запланировало новую попытку, результат не применять. */
    fun retriesFailedResolve(): Boolean = false

    /**
     * Результат резолва применяется к экрану.
     *
     * @return true, если этим резолвом завершилось фоновое восстановление.
     */
    fun onStreamResolved(result: PlayerStreamLoadResult.State, failed: Boolean): Boolean = false

    /** Успешный результат уже в состоянии экрана. */
    suspend fun afterStreamApplied(result: PlayerStreamLoadResult.State) {}

    /** Смена серии/источника: сбросить счётчики и отменить отложенные попытки. */
    fun reset()

    /** Освободить сессии источника. Отложенные попытки не отменяет — для этого [reset]. */
    fun close(immediately: Boolean = true) {}
}
