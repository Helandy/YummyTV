package su.afk.yummy.tv.feature.player.model

internal enum class PlayerControlFocusTarget {
    Dubbing,
    Balancer,
    Episodes,
    Quality,

    /** Боковое меню настроек справа: скорость, масштаб, субтитры и звук. */
    PlayerSettings,
    Alloha,

    /** Кнопка «следующая серия» в нижнем ряду. */
    NextEpisode,
}
