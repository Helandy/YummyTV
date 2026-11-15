package su.afk.yummy.tv.core.designsystem.mobile

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Тонкая обёртка над [HorizontalPager], привязанная к [MobileSwipeableTabsState].
 */
@Composable
fun MobileSwipeableTabsPager(
    state: MobileSwipeableTabsState,
    modifier: Modifier = Modifier,
    key: ((page: Int) -> Any)? = null,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    pageSpacing: Dp = 0.dp,
    pageContent: @Composable (page: Int) -> Unit,
) {
    HorizontalPager(
        state = state.pagerState,
        key = key,
        modifier = modifier,
        verticalAlignment = verticalAlignment,
        contentPadding = contentPadding,
        pageSpacing = pageSpacing,
    ) { page ->
        pageContent(page)
    }
}
