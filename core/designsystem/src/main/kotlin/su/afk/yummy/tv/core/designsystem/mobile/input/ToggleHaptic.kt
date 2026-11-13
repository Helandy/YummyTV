package su.afk.yummy.tv.core.designsystem.mobile.input

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * Вибро-отклик на переключатель (лайк, избранное): возвращает функцию, которую вызывают в `onClick`
 * с текущим значением — отклик соответствует тому состоянию, в которое элемент переходит.
 */
@Composable
fun rememberToggleHaptic(): (currentlySelected: Boolean) -> Unit {
    val haptic = LocalHapticFeedback.current
    return remember(haptic) {
        { currentlySelected ->
            haptic.performHapticFeedback(
                if (currentlySelected) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn,
            )
        }
    }
}
