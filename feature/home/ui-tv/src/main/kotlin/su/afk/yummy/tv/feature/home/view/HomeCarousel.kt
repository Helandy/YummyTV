package su.afk.yummy.tv.feature.home.view

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusOverlay
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.domain.home.model.HomeFeedItem

@Composable
internal fun HomeCarousel(
    items: List<HomeFeedItem>,
    onItemSelected: (sectionId: String, item: HomeFeedItem) -> Unit,
    sectionKey: String,
    modifier: Modifier = Modifier,
    rowFocusRequester: FocusRequester? = null,
    rowIsFocused: Boolean = false,
    upFocusRequester: FocusRequester? = null,
    downFocusRequester: FocusRequester? = null,
    onCarouselFocused: () -> Unit = {},
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
) {
    if (items.isEmpty()) return

    if (items.size == 1) {
        val item = items[0]
        val focusRequester = remember { FocusRequester() }
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = TvScreenPadding.Horizontal)
                .clip(RoundedCornerShape(12.dp))
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    when (event.key) {
                        Key.DirectionLeft,
                        Key.DirectionRight -> true

                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                            onItemSelected(sectionKey, item)
                            true
                        }

                        Key.DirectionUp -> {
                            onMoveUp?.invoke()
                            onMoveUp != null
                        }

                        Key.DirectionDown -> {
                            onMoveDown?.invoke()
                            onMoveDown != null
                        }

                        else -> false
                    }
                },
        ) {
            HeroBannerPage(
                item = item,
                onClick = { onItemSelected(sectionKey, item) },
                focusRequester = rowFocusRequester ?: focusRequester,
                upFocusRequester = upFocusRequester,
                downFocusRequester = downFocusRequester,
                forceFocused = rowIsFocused,
                onMoveLeft = {},
                onMoveRight = {},
                onFocused = {
                    onCarouselFocused()
                },
            )
        }
        return
    }

    val pagerState = rememberPagerState { items.size }
    val scope = rememberCoroutineScope()
    var isCarouselFocused by remember { mutableStateOf(false) }
    var pageHasFocus by remember { mutableStateOf(false) }
    var showCarouselFocus by remember { mutableStateOf(false) }
    var isRestoringFocus by remember { mutableStateOf(false) }
    var lastFocusedPage by rememberSaveable { mutableIntStateOf(0) }
    val pageRequesters = remember(items.size) { List(items.size) { FocusRequester() } }

    fun notifyPageFocused(page: Int) {
        lastFocusedPage = page.coerceIn(0, items.lastIndex)
    }

    fun moveToPreviousPage() {
        if (pagerState.isScrollInProgress) return
        isCarouselFocused = true
        showCarouselFocus = true
        scope.launch {
            onCarouselFocused()
            val previousPage = if (pagerState.currentPage == 0) {
                items.lastIndex
            } else {
                pagerState.currentPage - 1
            }
            pagerState.animateScrollToPage(previousPage)
            notifyPageFocused(previousPage)
            onCarouselFocused()
        }
    }

    fun moveToNextPage() {
        if (pagerState.isScrollInProgress) return
        isCarouselFocused = true
        showCarouselFocus = true
        scope.launch {
            onCarouselFocused()
            val nextPage = if (pagerState.currentPage == items.lastIndex) {
                0
            } else {
                pagerState.currentPage + 1
            }
            pagerState.animateScrollToPage(nextPage)
            notifyPageFocused(nextPage)
            onCarouselFocused()
        }
    }

    suspend fun requestPageFocus(page: Int) {
        val target = page.coerceIn(0, items.lastIndex)
        if (target != pagerState.currentPage) {
            pagerState.scrollToPage(target)
        }
        repeat(6) {
            runCatching { pageRequesters[target].requestFocus() }
            withFrameNanos { }
        }
        showCarouselFocus = true
        notifyPageFocused(target)
    }

    fun focusedPageIndex(): Int {
        return lastFocusedPage.coerceIn(0, items.lastIndex)
    }

    fun selectCurrentPage() {
        val page = pagerState.currentPage.coerceIn(0, items.lastIndex)
        val item = items[page]
        notifyPageFocused(page)
        onItemSelected(sectionKey, item)
    }

    // After page settles and carousel still focused — move focus to the new page's content
    LaunchedEffect(pagerState.currentPage, isCarouselFocused, isRestoringFocus) {
        if (isCarouselFocused && !isRestoringFocus) {
            onCarouselFocused()
            scope.launch { requestPageFocus(pagerState.currentPage) }
        }
    }

    LaunchedEffect(isCarouselFocused, pageHasFocus, rowIsFocused) {
        if (isCarouselFocused || pageHasFocus || rowIsFocused) return@LaunchedEffect
        while (true) {
            delay(5_000L)
            if (!pagerState.isScrollInProgress && !pageHasFocus && !rowIsFocused) {
                val next = (pagerState.currentPage + 1) % items.size
                pagerState.animateScrollToPage(next)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = TvScreenPadding.Horizontal)
            .clip(RoundedCornerShape(12.dp))
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        moveToPreviousPage()
                        true
                    }

                    Key.DirectionRight -> {
                        moveToNextPage()
                        true // always consume — keep focus inside carousel
                    }

                    Key.DirectionUp -> {
                        showCarouselFocus = false
                        onMoveUp?.invoke()
                        onMoveUp != null
                    }

                    Key.DirectionDown -> {
                        showCarouselFocus = false
                        onMoveDown?.invoke()
                        onMoveDown != null
                    }

                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        selectCurrentPage()
                        true
                    }

                    else -> false
                }
            }
            .then(if (rowFocusRequester != null) Modifier.focusRequester(rowFocusRequester) else Modifier)
            .tvFocusRestorer(
                fallback = pageRequesters.getOrNull(focusedPageIndex())
                    ?: rowFocusRequester
                    ?: FocusRequester.Default,
            )
            .focusable()
            .focusProperties {
                upFocusRequester?.let { up = it }
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
                downFocusRequester?.let { down = it }
            }
            .onFocusChanged { state ->
                val hadFocus = isCarouselFocused
                isCarouselFocused = state.hasFocus
                if (state.hasFocus && !hadFocus) {
                    onCarouselFocused()
                    isRestoringFocus = true
                    scope.launch {
                        val target = focusedPageIndex()
                        requestPageFocus(target)
                        isRestoringFocus = false
                    }
                }
                if (!state.hasFocus) {
                    isRestoringFocus = false
                }
            },
    ) {
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = false,
            modifier = Modifier.fillMaxWidth(),
        ) { page ->
            val item = items[page]
            HeroBannerPage(
                item = item,
                onClick = { onItemSelected(sectionKey, item) },
                focusRequester = pageRequesters[page],
                upFocusRequester = upFocusRequester,
                downFocusRequester = downFocusRequester,
                forceFocused = page == pagerState.currentPage &&
                        (rowIsFocused || showCarouselFocus || isCarouselFocused),
                onMoveLeft = ::moveToPreviousPage,
                onMoveRight = ::moveToNextPage,
                onFocused = {
                    pageHasFocus = true
                    showCarouselFocus = true
                    onCarouselFocused()
                    notifyPageFocused(page)
                },
                onFocusChanged = { focused ->
                    pageHasFocus = focused
                    if (focused) {
                        showCarouselFocus = true
                    }
                },
            )
        }

        TvFocusOverlay(
            focused = rowIsFocused || showCarouselFocus || isCarouselFocused,
            modifier = Modifier
                .zIndex(1f)
                .fillMaxSize()
                .padding(2.dp),
        )
    }
}
