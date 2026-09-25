package su.afk.yummy.tv.feature.player.handler

import javax.inject.Inject

/**
 * Считает тихие авто-повторы воспроизведения для не-Alloha плееров.
 *
 * Бюджет попыток - на один непрерывный сеанс воспроизведения: сбрасывается при удачном старте
 * ([reset] по STATE_READY) и при смене эпизода/озвучки/качества/балансера. После исчерпания лимита
 * юзеру показывается обычный оверлей ошибки.
 */
internal class PlayerPlaybackRetryHandler @Inject constructor() {
    var attempts: Int = 0
        private set

    fun canRetry(): Boolean = attempts < MAX_ATTEMPTS

    fun next(): Int = ++attempts

    fun reset() {
        attempts = 0
    }

    companion object {
        const val MAX_ATTEMPTS = 5
    }
}
