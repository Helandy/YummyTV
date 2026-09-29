package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.model.PanelReturnFocusTarget
import su.afk.yummy.tv.feature.player.model.PlayerControlFocusTarget

internal fun String.withoutDubbingTitlePrefix(title: String): String {
    val trimmed = trim()
    if (!trimmed.startsWith(title, ignoreCase = true)) return trimmed

    val withoutTitle = trimmed
        .drop(title.length)
        .trimStart()
        .trimStart(':', '-')
        .trimStart()

    return withoutTitle.ifBlank { trimmed }
}

internal fun PanelReturnFocusTarget.toPlayerControlFocusTarget(): PlayerControlFocusTarget =
    when (this) {
        PanelReturnFocusTarget.Quality -> PlayerControlFocusTarget.Quality
        PanelReturnFocusTarget.Dubbing -> PlayerControlFocusTarget.Dubbing
        PanelReturnFocusTarget.Balancer -> PlayerControlFocusTarget.Balancer
        PanelReturnFocusTarget.Resize -> PlayerControlFocusTarget.Resize
        PanelReturnFocusTarget.Speed -> PlayerControlFocusTarget.Speed
        PanelReturnFocusTarget.Volume -> PlayerControlFocusTarget.Volume
        PanelReturnFocusTarget.Alloha -> PlayerControlFocusTarget.Alloha
    }

internal fun Float.speedLabel(): String =
    if (this % 1f == 0f) "${toInt()}x" else "${this}x"
