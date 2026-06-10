package su.afk.yummy.tv.feature.home.view

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.TvFocusOverlay
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPosterQuality
import su.afk.yummy.tv.domain.anime.model.AnimePreview
import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.HomeFeedItemAction
import su.afk.yummy.tv.feature.home.R

@Composable
internal fun HomeCarousel(
    items: List<HomeFeedItem>,
    onItemSelected: (sectionId: String, item: HomeFeedItem) -> Unit,
    onItemFocused: (sectionId: String, displayId: Int, animeId: Int?) -> Unit,
    onItemVisible: (displayId: Int) -> Unit,
    sectionKey: String,
    focusedSectionId: String?,
    focusedItemId: Int?,
    focusedPreview: AnimePreview?,
    animePreviews: Map<Int, AnimePreview>,
    modifier: Modifier = Modifier,
    rowFocusRequester: FocusRequester? = null,
    rowIsFocused: Boolean = false,
    restoreFocusedPageOnEnter: Boolean = false,
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
        LaunchedEffect(item.id) {
            onItemVisible(item.id)
        }
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
                preview = if (item.id == focusedItemId) {
                    focusedPreview ?: item.animeId?.let(animePreviews::get)
                } else {
                    item.animeId?.let(animePreviews::get)
                },
                onClick = { onItemSelected(sectionKey, item) },
                focusRequester = rowFocusRequester ?: focusRequester,
                upFocusRequester = upFocusRequester,
                downFocusRequester = downFocusRequester,
                forceFocused = rowIsFocused,
                onMoveLeft = {},
                onMoveRight = {},
                onFocused = {
                    onCarouselFocused()
                    val animeId = (item.action as? HomeFeedItemAction.OpenSeries)?.seriesId
                    onItemFocused(sectionKey, item.id, animeId)
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
    val pageRequesters = remember(items.size) { List(items.size) { FocusRequester() } }

    fun notifyPageFocused(page: Int) {
        val item = items.getOrNull(page) ?: return
        onItemFocused(sectionKey, item.id, item.animeId)
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
        if (focusedSectionId != sectionKey) return pagerState.currentPage
        val focusedPage = items.indexOfFirst { it.id == focusedItemId }
        return focusedPage.takeIf { it >= 0 } ?: pagerState.currentPage
    }

    fun selectCurrentPage() {
        val page = pagerState.currentPage.coerceIn(0, items.lastIndex)
        val item = items[page]
        onItemFocused(sectionKey, item.id, item.animeId)
        onItemSelected(sectionKey, item)
    }

    LaunchedEffect(focusedSectionId, focusedItemId, items) {
        if (focusedSectionId != sectionKey) return@LaunchedEffect
        val focusedPage = items.indexOfFirst { it.id == focusedItemId }
        if (!isCarouselFocused && focusedPage >= 0 && focusedPage != pagerState.currentPage) {
            pagerState.scrollToPage(focusedPage)
        }
    }

    LaunchedEffect(pagerState.currentPage, items) {
        items.getOrNull(pagerState.currentPage)?.let { item ->
            onItemVisible(item.id)
        }
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
            .focusable()
            .focusProperties {
                upFocusRequester?.let { up = it }
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
                downFocusRequester?.let { down = it }
            }
            .focusGroup()
            .onFocusChanged { state ->
                val hadFocus = isCarouselFocused
                isCarouselFocused = state.hasFocus
                if (state.hasFocus && !hadFocus) {
                    onCarouselFocused()
                    isRestoringFocus = true
                    scope.launch {
                        val target = if (restoreFocusedPageOnEnter) {
                            focusedPageIndex()
                        } else {
                            pagerState.currentPage
                        }.coerceIn(0, items.lastIndex)
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
            val preview = when {
                page == pagerState.currentPage &&
                        focusedSectionId == sectionKey &&
                        item.id == focusedItemId -> focusedPreview ?: item.animeId?.let(
                    animePreviews::get
                )
                page in (pagerState.currentPage - 1)..(pagerState.currentPage + 1) -> item.animeId?.let(animePreviews::get)
                else -> null
            }
            HeroBannerPage(
                item = item,
                preview = preview,
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
                    val animeId = (item.action as? HomeFeedItemAction.OpenSeries)?.seriesId
                    onItemFocused(sectionKey, item.id, animeId)
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

private val HomeFeedItem.animeId: Int?
    get() = (action as? HomeFeedItemAction.OpenSeries)?.seriesId

@Composable
private fun HeroBannerPage(
    item: HomeFeedItem,
    preview: AnimePreview?,

    onClick: () -> Unit,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    downFocusRequester: FocusRequester?,
    forceFocused: Boolean,
    onMoveLeft: () -> Unit,
    onMoveRight: () -> Unit,
    onFocused: () -> Unit,
    onFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val posterQuality = LocalPosterQuality.current
    val surfaceColor = MaterialTheme.colorScheme.surface
    var isFocused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    var showSkeleton by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(preview) {
        if (preview == null && item.action is HomeFeedItemAction.OpenSeries) {
            delay(400)
            showSkeleton = true
        } else {
            showSkeleton = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .background(surfaceColor)
            .focusRequester(focusRequester)
            .focusProperties {
                upFocusRequester?.let { up = it }
                left = FocusRequester.Cancel
                right = FocusRequester.Cancel
                downFocusRequester?.let { down = it }
            }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.DirectionLeft -> {
                        onMoveLeft()
                        true
                    }

                    Key.DirectionRight -> {
                        onMoveRight()
                        true
                    }

                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> {
                        onClick()
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged {
                val focused = it.isFocused || it.hasFocus
                if (focused && !isFocused) onFocused()
                if (focused != isFocused) onFocusChanged(focused)
                isFocused = focused
            }
            .focusable(interactionSource = interactionSource)
            .tvFocusableClick(
                onClick = onClick,
                interactionSource = interactionSource,
                shape = RoundedCornerShape(12.dp),
                focusedScale = 1f,
            ),
    ) {
        // Poster — full height, right-aligned, no vertical padding
        Box(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .align(Alignment.CenterEnd)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            AsyncImage(
                model = item.posterUrl(posterQuality),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Gradient blending poster into the text area
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(surfaceColor, Color.Transparent),
                        ),
                    ),
            )
        }

        // Text content — vertically centered, doesn't overlap poster
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 24.dp, end = 216.dp, top = 32.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        ) {
            item.rating?.let { rating ->
                Text(
                    text = stringResource(R.string.home_rating, "%.1f".format(rating)),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            when {
                preview != null -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        preview.year?.let { HeroBadge(it.toString()) }
                        preview.ageRating?.let { HeroBadge(it) }
                        preview.type?.let { HeroBadge(it) }
                        preview.season?.let { HeroBadge(stringResource(R.string.home_season, it)) }
                    }
                    if (preview.genres.isNotEmpty()) {
                        Text(
                            text = preview.genres.take(3).joinToString(" · "),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (preview.description.isNotBlank()) {
                        Text(
                            text = preview.description,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                showSkeleton -> PreviewSkeleton()
                item.description.isNotBlank() -> Text(
                    text = item.description,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }

        TvFocusOverlay(
            focused = isFocused || forceFocused,
            modifier = Modifier
                .zIndex(1f)
                .fillMaxSize()
                .padding(2.dp),
        )
    }
}

@Composable
private fun PreviewSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "alpha",
    )
    val color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(3) {
                Box(
                    Modifier
                        .width(52.dp)
                        .height(20.dp)
                        .background(color, RoundedCornerShape(4.dp)),
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth(0.55f)
                .height(13.dp)
                .background(color, RoundedCornerShape(4.dp)),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(3) { i ->
                Box(
                    Modifier
                        .fillMaxWidth(if (i == 2) 0.45f else 0.85f)
                        .height(12.dp)
                        .background(color, RoundedCornerShape(4.dp)),
                )
            }
        }
    }
}

@Composable
private fun HeroBadge(text: String) {
    Text(
        text = text,
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
