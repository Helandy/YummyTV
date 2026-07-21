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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import kotlin.time.Duration.Companion.seconds

private val HERO_AUTO_SCROLL_INTERVAL = 5.seconds

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
                val nextPage = (pagerState.currentPage + 1) % items.size
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
            pageSpacing = 12.dp,
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
