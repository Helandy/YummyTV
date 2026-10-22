package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.MobilePosterGridHorizontalPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterSpacing
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterWidth

@Composable
fun MobilePosterGrid(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    /** Минимальная ширина ячейки; по умолчанию — ширина постера (широкие карточки задают свою). */
    minCellWidth: Dp = currentMobilePosterWidth(),
    /** Одна колонка на всю ширину — для построчных карточек. */
    singleColumn: Boolean = false,
    content: LazyGridScope.() -> Unit,
) {
    val spacing = currentMobilePosterSpacing()

    LazyVerticalGrid(
        columns = if (singleColumn) GridCells.Fixed(1) else GridCells.Adaptive(minCellWidth),
        state = state,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MobilePosterGridHorizontalPadding,
            top = contentPadding.calculateTopPadding() + 12.dp,
            end = MobilePosterGridHorizontalPadding,
            bottom = contentPadding.calculateBottomPadding() + 16.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing),
        content = content,
    )
}
