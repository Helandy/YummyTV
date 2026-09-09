package su.afk.yummy.tv.core.designsystem.baseScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp

/**
 * Гасит остаточный scroll/fling, когда список внутри шторки короче её максимальной высоты и
 * упирается в свой нижний край: без этого остаток жеста уходит через nested scroll в сам
 * `ModalBottomSheet`, и тот дёргается вверх и потом долго анимированно возвращается обратно.
 * Жест "потянуть вниз от начала списка" (закрытие свайпом) не трогаем — пропускаем как есть.
 */
@Composable
private fun rememberBottomOverscrollGuard(): NestedScrollConnection = remember {
    object : NestedScrollConnection {
        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset = if (available.y < 0f) available else Offset.Zero

        override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
            if (available.y < 0f) available else Velocity.Zero
    }
}

/**
 * Вариант [BaseBottomSheet] для контента, который сам управляет своим корневым layout'ом
 * (например, [androidx.compose.foundation.lazy.LazyColumn] с собственными insets/contentPadding).
 * [content] получает [maxHeight] сам ограничивает себя через `Modifier.heightIn(max = maxHeight)`.
 *
 * `contentWindowInsets` у материала отключены (почему - см. [BaseBottomSheet]), поэтому нижний
 * инсет [content] обязан применить сам: `Modifier.windowInsetsPadding(WindowInsets.navigationBars)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseBottomSheetCustom(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    content: @Composable (maxHeight: Dp) -> Unit,
) {
    val maxHeight = rememberBottomSheetMaxHeight()

    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        Box(modifier = Modifier.nestedScroll(rememberBottomOverscrollGuard())) {
            content(maxHeight)
        }
    }
}
