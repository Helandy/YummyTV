package su.afk.yummy.tv.core.designsystem.mobile.input

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput

/** Курсор-«рука» над кликабельным элементом при работе мышью. */
fun Modifier.clickablePointer(): Modifier = pointerHoverIcon(PointerIcon.Hand)

/**
 * Правый клик мышью — то же действие, что долгое нажатие пальцем (контекстное меню).
 * Без [action] модификатор ничего не делает, чтобы не глотать события у элементов без меню.
 */
fun Modifier.onSecondaryClick(action: (() -> Unit)?): Modifier {
    if (action == null) return this
    return pointerInput(action) {
        awaitEachGesture {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                event.changes.forEach { it.consume() }
                action()
            }
        }
    }
}
