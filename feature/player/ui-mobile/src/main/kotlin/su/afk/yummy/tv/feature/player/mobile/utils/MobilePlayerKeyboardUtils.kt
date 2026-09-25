package su.afk.yummy.tv.feature.player.mobile.utils

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import su.afk.yummy.tv.feature.player.mobile.model.MobilePlayerKeyAction

/**
 * Раскладка как у веб-плееров: пробел/K — пауза, ←/J и →/L — шаг перемотки. Сочетания с
 * модификаторами не трогаем, чтобы не перехватывать системные и глобальные (Ctrl+F).
 */
internal fun KeyEvent.toMobilePlayerKeyAction(): MobilePlayerKeyAction? {
    if (type != KeyEventType.KeyDown || isCtrlPressed || isAltPressed || isMetaPressed) return null
    return when (key) {
        Key.Spacebar, Key.K -> MobilePlayerKeyAction.PlayPause
        Key.DirectionLeft, Key.J -> MobilePlayerKeyAction.SeekBackward
        Key.DirectionRight, Key.L -> MobilePlayerKeyAction.SeekForward
        else -> null
    }
}
