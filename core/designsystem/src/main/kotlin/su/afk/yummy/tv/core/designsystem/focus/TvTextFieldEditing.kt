package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController

/**
 * Режим правки текстового поля с пульта. Пока поле просто в фокусе, оно `readOnly` и клавиатура скрыта: иначе каждое
 * попадание в поле при навигации стрелками выезжает клавиатурой, окно пересчитывает размеры и экран «дрожит».
 * Клавиатура и правка включаются по OK/Enter, выключаются по «Назад» или действию IME «Готово» ([stop]).
 */
@Stable
class TvTextFieldEditing internal constructor(
    private val keyboard: SoftwareKeyboardController?,
    private val focusManager: FocusManager,
) {
    var isEditing by mutableStateOf(false)
        private set

    fun start() {
        isEditing = true
        keyboard?.show()
    }

    fun stop() {
        isEditing = false
        keyboard?.hide()
    }

    /** Однострочное поле само съедает «вверх/вниз», поэтому вне правки фокус переводим вручную. */
    internal fun moveFocus(direction: FocusDirection): Boolean = !isEditing && focusManager.moveFocus(direction)

    internal fun onFocusChanged(focused: Boolean) {
        if (focused && !isEditing) {
            keyboard?.hide()
        } else if (!focused) {
            isEditing = false
        }
    }
}

@Composable
fun rememberTvTextFieldEditing(): TvTextFieldEditing {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    return remember(keyboard, focusManager) { TvTextFieldEditing(keyboard, focusManager) }
}

/** Подключает [editing] к полю: прячет клавиатуру при фокусе, начинает правку по OK/Enter, заканчивает по «Назад». */
fun Modifier.tvTextFieldEditing(editing: TvTextFieldEditing): Modifier = this
    .onFocusChanged { editing.onFocusChanged(it.isFocused) }
    .onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionCenter, Key.Enter, Key.NumPadEnter ->
                if (!editing.isEditing) {
                    editing.start()
                    true
                } else {
                    false
                }

            Key.Back ->
                if (editing.isEditing) {
                    editing.stop()
                    true
                } else {
                    false
                }

            Key.DirectionDown -> editing.moveFocus(FocusDirection.Down)
            Key.DirectionUp -> editing.moveFocus(FocusDirection.Up)

            else -> false
        }
    }
