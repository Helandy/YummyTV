package su.afk.yummy.tv.core.designsystem.baseScreen

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val MAX_HEIGHT_FRACTION = 0.85f

/** Высота `BottomSheetDefaults.DragHandle`: полоска 4dp + вертикальные отступы 22dp. */
private val DRAG_HANDLE_HEIGHT = 48.dp

/**
 * Высота, дальше которой контент bottom sheet не должен растягиваться (85% высоты, доступной
 * под статус-баром). Считать долю от полной [LocalConfiguration.screenHeightDp] без вычета
 * статус-бара нельзя: на широких/невысоких окнах (например, разложенный foldable) 85% полного
 * экрана может превышать высоту под статус-баром, и шторка заезжает под него.
 *
 * Вычитаем и drag handle: доля должна описывать всю шторку целиком, а не только её контент -
 * иначе в ландшафте, где вся высота окна ~450dp, ручка съедала заметную часть свободной полосы.
 * Остальную геометрию шторка себе не добавляет: [BaseBottomSheet] и [BaseBottomSheetCustom]
 * отключают материаловские `contentWindowInsets`, иначе появившийся статус-бар прибавлялся бы
 * к этому лимиту сверху и шторка уезжала бы вверх уже после открытия.
 */
@Composable
fun rememberBottomSheetMaxHeight(): Dp {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val availableHeight =
        LocalConfiguration.current.screenHeightDp.dp - statusBarHeight - DRAG_HANDLE_HEIGHT
    return (availableHeight * MAX_HEIGHT_FRACTION).coerceAtLeast(0.dp)
}
