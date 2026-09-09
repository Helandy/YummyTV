package su.afk.yummy.tv.core.designsystem.baseScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
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
 * Общая обёртка над [ModalBottomSheet]: заголовок, стандартные отступы и высота не более
 * [rememberBottomSheetMaxHeight].
 *
 * `contentWindowInsets` у материала отключены: по умолчанию он сам вешает внутрь шторки
 * `windowInsetsPadding(safeDrawing.only(Top + Bottom))`, и этот паддинг лежит снаружи нашего
 * `heightIn(max)` — появившийся статус-бар просто прибавлялся бы к лимиту, и шторка уезжала бы
 * вверх (заметнее всего в ландшафте). Нижний инсет поэтому применяем сами.
 *
 * [scrollableContent] включать, если [content] не содержит собственного скролла: тогда контент,
 * не влезающий в лимит высоты, можно будет прокрутить, а не обрежется. С `LazyColumn` внутри
 * включать нельзя — краш "infinity maximum height constraints".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BaseBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    title: String? = null,
    titleContent: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    scrollableContent: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val maxHeight = rememberBottomSheetMaxHeight()
    val scrollState = rememberScrollState()

    ModalBottomSheet(
        modifier = modifier,
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        contentWindowInsets = { WindowInsets(0.dp) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxHeight)
                .navigationBarsPadding()
                .padding(contentPadding)
                .nestedScroll(rememberBottomOverscrollGuard())
                .then(
                    if (scrollableContent) Modifier.verticalScroll(scrollState) else Modifier,
                ),
            verticalArrangement = verticalArrangement,
        ) {
            when {
                titleContent != null -> titleContent()
                title != null -> Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 14.dp),
                )
            }
            content()
        }
    }
}
