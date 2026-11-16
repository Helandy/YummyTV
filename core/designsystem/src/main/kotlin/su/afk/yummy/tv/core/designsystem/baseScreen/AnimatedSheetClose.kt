package su.afk.yummy.tv.core.designsystem.baseScreen

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch

/**
 * Закрыть шторку с анимацией и только потом выполнить [after] (обычно — убрать её из композиции).
 * Без анимации шторка при программном закрытии (выбор пункта, тап по скриму) исчезает за кадр.
 * Вне [BaseBottomSheet] выполняет [after] сразу.
 */
typealias SheetCloser = (after: () -> Unit) -> Unit

val LocalSheetCloser = compositionLocalOf<SheetCloser> { { after -> after() } }

/** Возвращает [SheetCloser] для [sheetState]: `hide()` → [after]; отмена (шторку убрали) [after] не зовёт. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberSheetCloser(sheetState: SheetState): SheetCloser {
    val scope = rememberCoroutineScope()
    return remember(sheetState, scope) {
        { after ->
            scope.launch { sheetState.hide() }.invokeOnCompletion { cause ->
                if (cause == null) after()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun rememberAnimatedDismiss(sheetState: SheetState, onDismissRequest: () -> Unit): () -> Unit {
    val closer = rememberSheetCloser(sheetState)
    val latest by rememberUpdatedState(onDismissRequest)
    return remember(closer) { { closer { latest() } } }
}
