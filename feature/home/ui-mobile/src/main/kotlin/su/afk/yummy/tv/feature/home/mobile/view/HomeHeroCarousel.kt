package su.afk.yummy.tv.feature.home.mobile.view

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

private val HERO_AUTO_SCROLL_INTERVAL = 5.seconds

/** Автосмена без рывка: дефолтная пружина перекидывает узкую карточку почти мгновенно. */
private const val HERO_AUTO_SCROLL_DURATION_MS = 900
private val HERO_PAGE_SPACING = 10.dp

/** Отступ по краям пейджера: из-под него выглядывают соседние карточки. */
private val HERO_PEEK_PADDING = 44.dp
private const val HERO_INACTIVE_SCALE = 0.88f
private const val HERO_INACTIVE_ALPHA = 0.75f

/** На телефоне карточка во всю ширину, на планшете не шире [HERO_MAX_PAGE_WIDTH] — видно несколько. */
private val HERO_MAX_PAGE_WIDTH = 520.dp

private object HeroPageSize : PageSize {
    override fun Density.calculateMainAxisPageSize(availableSpace: Int, pageSpacing: Int): Int =
        minOf(availableSpace, HERO_MAX_PAGE_WIDTH.roundToPx())
}

@Composable
internal fun HomeHeroCarousel(
    items: List<HomeFeedItem>,
    onItemSelected: (HomeFeedItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState { items.size }
    var isUserTouchingCarousel by remember { mutableStateOf(false) }
    // Пользователь сам листал — дальше карусель не перехватывает управление. Saveable, чтобы
    // автопрокрутка не ожила, когда карусель уезжает за экран ленты и возвращается
    var isAutoScrollCancelled by rememberSaveable { mutableStateOf(false) }
    val isUserDragging by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(isUserDragging) {
        if (isUserDragging) isAutoScrollCancelled = true
    }

    LaunchedEffect(items.size, isUserTouchingCarousel, isAutoScrollCancelled) {
        if (items.size <= 1 || isUserTouchingCarousel || isAutoScrollCancelled) {
            return@LaunchedEffect
        }
        while (true) {
            delay(HERO_AUTO_SCROLL_INTERVAL)
            if (!isUserTouchingCarousel && !pagerState.isScrollInProgress) {
                pagerState.animateScrollToPage(
                    page = (pagerState.currentPage + 1) % items.size,
                    animationSpec = tween(
                        durationMillis = HERO_AUTO_SCROLL_DURATION_MS,
                        easing = FastOutSlowInEasing,
                    ),
                )
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
            contentPadding = PaddingValues(horizontal = HERO_PEEK_PADDING),
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val item = items[page]
            HomeHeroCard(
                item = item,
                onClick = { onItemSelected(item) },
                modifier = Modifier.graphicsLayer {
                    val offset = pagerState.getOffsetDistanceInPages(page).absoluteValue
                        .coerceIn(0f, 1f)
                    // Только по высоте: сжатие по ширине съело бы выглядывающий край соседа
                    scaleY = lerp(1f, HERO_INACTIVE_SCALE, offset)
                    alpha = lerp(1f, HERO_INACTIVE_ALPHA, offset)
                },
            )
        }
    }
}
