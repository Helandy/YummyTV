package su.afk.yummy.tv.core.designsystem.baseScreen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Прячет системные бары в окне самой шторки.
 *
 * `ModalBottomSheet` живёт в отдельном окне диалога и immersive-режим экрана-хозяина не наследует:
 * material3 в этом окне только красит бары под цвет контента, но не прячет их. Из-за этого на
 * полноэкранном плеере открытие шторки выводит на экран статус-бар и навбар, инсеты внутри окна
 * шторки перестают быть нулевыми, и шторка (прибитая к нижнему краю) уезжает вверх сразу после
 * открытия.
 *
 * Вызывать первой строкой контента шторки - и только на экранах, которые сами прячут системные бары.
 */
@Composable
fun HideSheetWindowSystemBars() {
    val view = LocalView.current
    val window = (view.parent as? DialogWindowProvider)?.window

    DisposableEffect(window, view) {
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            ?: return@DisposableEffect onDispose { }

        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())

        onDispose { }
    }
}
