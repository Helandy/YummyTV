package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import kotlin.time.Duration.Companion.seconds

private val HERO_AUTO_SCROLL_INTERVAL = 5.seconds
private val HERO_PAGE_SPACING = 12.dp

/** Уже этой ширины карточка сезона выглядит тесно: на телефоне две на экран, шире — три. */
private val HERO_MIN_PAGE_WIDTH = 150.dp
private const val HERO_MAX_VISIBLE_PAGES = 3

/**
 * Сколько карточек влезает в ширину пейджера: одна растянутая карточка выглядит пусто, поэтому
 * делим ширину на столько карточек, сколько позволяет [HERO_MIN_PAGE_WIDTH].
 */
private object HeroPageSize : PageSize {
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int {
        val minPage = HERO_MIN_PAGE_WIDTH.roundToPx()
        val count = ((availableSpace + pageSpacing) / (minPage + pageSpacing))
            .coerceIn(1, HERO_MAX_VISIBLE_PAGES)
        return (availableSpace - pageSpacing * (count - 1)) / count
    }
}

@Composable
internal fun HomeHeroCarousel(
    items: List<HomeFeedItem>,
    onItemSelected: (HomeFeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { items.size }
    var isUserTouchingCarousel by remember { mutableStateOf(false) }

    LaunchedEffect(items.size, isUserTouchingCarousel) {
        if (items.size <= 1 || isUserTouchingCarousel) return@LaunchedEffect
        while (true) {
            delay(HERO_AUTO_SCROLL_INTERVAL)
            if (!isUserTouchingCarousel && !pagerState.isScrollInProgress) {
                // При нескольких карточках на экране currentPage в конце упирается раньше
                // последнего индекса — поэтому на круг уходим, когда вперёд листать некуда.
                val nextPage = if (pagerState.canScrollForward) pagerState.currentPage + 1 else 0
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                try {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            isUserTouchingCarousel = event.changes.any { it.pressed }
                        }
                    }
                } finally {
                    isUserTouchingCarousel = false
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            key = { page -> items[page].id },
            pageSize = HeroPageSize,
            pageSpacing = HERO_PAGE_SPACING,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val item = items[page]
            HomeHeroCard(
                item = item,
                onClick = { onItemSelected(item) },
            )
        }

        if (items.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEachIndexed { index, _ ->
                    val selected = index == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .size(width = if (selected) 18.dp else 6.dp, height = 6.dp)
                            .clip(CircleShape)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                            ),
                    )
                }
            }
        }
    }
}
