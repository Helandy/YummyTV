package su.afk.yummy.tv.feature.player.host

import su.afk.yummy.tv.feature.player.navigator.PlayerDestination

/**
 * Отдельная Activity, в которой живёт мобильный плеер (чтобы остальное приложение оставалось
 * доступным под системным PiP-окном). Пока она зарегистрирована в [PlayerHostNavigation],
 * ключ плеера хранится в ней, а не в общем back stack.
 */
interface PlayerActivityHost {
    /** Текущая серия сменилась: запоминаем ключ, чтобы пережить пересоздание Activity. */
    fun onPlayerDestinationChanged(destination: PlayerDestination)

    /** Закрыть плеер и вернуться к приложению под ним. */
    fun finishPlayer()
}
