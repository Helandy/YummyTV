package su.afk.yummy.tv.feature.details.details.view

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.posterUrls
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.mapper.replacedAction
import su.afk.yummy.tv.feature.details.details.mapper.toDetailsMinimalTabs
import su.afk.yummy.tv.feature.details.details.model.DetailsMinimalTab
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.details.utils.tryRequestFocus
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.presentation.R

/**
 * Упрощённая деталка: шапка (постер, название, кнопки), под ней ряд вкладок — по одной на каждый пункт обычной
 * деталки — и содержимое выбранной вкладки. Фокус ходит по вертикали: кнопки → вкладки → содержимое.
 *
 * Экран «прокручивается» сам: когда фокус уходит в содержимое, шапка уезжает вверх, вкладки встают к верхнему краю,
 * и содержимому достаётся весь экран — внутри работают обычные вертикальные списки и сетки. Фокус вернулся на
 * вкладки или кнопки — шапка возвращается. Системную прокрутку за фокусом здесь не используем: смещение шапки
 * задаётся явно, поэтому экран не дёргается.
 */
@Composable
internal fun DetailsMinimalBody(
    state: DetailsState.State,
    details: AnimeDetails,
    videosState: VideosUiState,
    isWatchLoading: Boolean,
    watchProgress: DetailsWatchProgressIndex,
    isInLibrary: Boolean,
    isFavorite: Boolean,
    libraryList: UserAnimeList?,
    userRating: Int?,
    canSubscribe: Boolean,
    restoreButtonFocusRequest: Int,
    onEvent: (DetailsState.Event) -> Unit,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onSubscriptionsSelected: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
    onRatingScreenSelected: () -> Unit,
    onCollectionsSelected: () -> Unit,
    onCommentsSelected: () -> Unit,
    onReviewsSelected: () -> Unit,
    onBloggerVideosSelected: () -> Unit,
    onRandomAnimeSelected: (() -> Unit)?,
    isRandomAnimeLoading: Boolean,
) {
    val barFirstFocus = remember { FocusRequester() }
    val barLastRowFocus = remember { FocusRequester() }
    val tabRequesters = remember { DetailsMinimalTab.entries.associateWith { FocusRequester() } }
    val contentFocus = remember { FocusRequester() }

    val hasScreenshots = state.screenshots.isNotEmpty()
    val tabs = remember(
        state.sections,
        hasScreenshots,
        state.trailers.size,
        details.viewingOrder.size,
        state.collections.size,
        details.reviewsCount,
    ) {
        state.sections.toDetailsMinimalTabs(
            hasScreenshots = hasScreenshots,
            hasVideo = state.trailers.isNotEmpty(),
            hasUniverse = details.viewingOrder.size > 1,
            hasCollections = state.collections.isNotEmpty(),
            hasReviews = details.reviewsCount > 0,
        )
    }
    var selectedTab by rememberSaveable { mutableStateOf(DetailsMinimalTab.EPISODES) }
    val currentTab = selectedTab.takeIf { it in tabs } ?: tabs.first()
    // Показ вкладки отстаёт от фокуса: пробег стрелкой по ряду не грузит каждую вкладку по пути.
    var shownTab by rememberSaveable { mutableStateOf(currentTab) }
    LaunchedEffect(currentTab) {
        if (shownTab != currentTab) {
            delay(TAB_SHOW_DELAY_MILLIS)
            shownTab = currentTab
        }
    }
    val displayedTab = shownTab.takeIf { it in tabs } ?: currentTab
    // Вход в ряд вкладок — всегда на выбранную.
    val tabsFocus = tabRequesters.getValue(currentTab)

    // Кнопки пунктов, ставших вкладками, из панели убираем. Набор постоянный, а не по текущим вкладкам: панель
    // забирает фокус себе, когда меняется число кнопок, а вкладки появляются по мере загрузки данных.
    // Видеообзоры — кнопкой, только если они есть (число приходит вместе с деталями, набор от этого не скачет).
    val hasBloggerVideos = details.bloggerVideosCount > 0
    val hiddenActions = remember(hasBloggerVideos) {
        DetailsMinimalTab.entries.map { it.replacedAction }.toSet() +
            if (hasBloggerVideos) emptySet() else setOf(DetailsButtonAction.BLOGGER_VIDEOS)
    }

    var contentHasFocus by remember { mutableStateOf(false) }
    // «Назад» из содержимого — на вкладки, а не сразу закрывать деталку. Объявлен до содержимого: обработчики
    // встроенных экранов (например, оверлеи серий) регистрируются позже и перехватывают «Назад» первыми.
    BackHandler(enabled = contentHasFocus) { tabsFocus.tryRequestFocus() }

    // Где был фокус, когда экран ушёл на паузу (плеер, подэкран). Меняется только на паузе: панель кнопок
    // перерегистрирует наблюдатель при смене focusOnResume и тут же получает ON_RESUME — фокус уезжал бы на кнопки.
    var pausedInContent by rememberSaveable { mutableStateOf(false) }
    // Разрешение программно войти в содержимое (восстановление после паузы). Без него вход — только стрелкой вниз.
    val allowContentEnter = remember { booleanArrayOf(false) }
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> pausedInContent = contentHasFocus
                // Кнопки восстанавливает сама панель, содержимое — здесь (его tvFocusRestorer вернёт ту же карточку).
                Lifecycle.Event.ON_RESUME -> if (pausedInContent) {
                    scope.launch {
                        allowContentEnter[0] = true
                        try {
                            requestFocusUntilTimeout(contentFocus)
                        } finally {
                            allowContentEnter[0] = false
                        }
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var heroHeightPx by remember { mutableIntStateOf(0) }
    var tabsHeightPx by remember { mutableIntStateOf(0) }
    val gapPx = with(LocalDensity.current) { SectionGap.roundToPx() }
    val collapseOffset by animateIntAsState(
        targetValue = if (contentHasFocus) heroHeightPx + gapPx else 0,
        label = "minimalHeroCollapse",
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .clipToBounds(),
    ) {
        Box(modifier = Modifier.matchParentSize()) { HeroBackdrop(details) }

        val tabsHeight = with(LocalDensity.current) { tabsHeightPx.toDp() }
        // Содержимое — на весь экран под вкладками, когда шапка уехала.
        val contentHeight = (maxHeight - TopPadding - BottomPadding - SectionGap - tabsHeight).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(align = Alignment.Top, unbounded = true)
                .offset { IntOffset(0, -collapseOffset) }
                .padding(top = TopPadding, bottom = BottomPadding),
        ) {
            MinimalHero(
                details = details,
                modifier = Modifier
                    .padding(horizontal = HorizontalPadding)
                    .onSizeChanged { heroHeightPx = it.height },
            ) {
                DetailsButtonBar(
                    details = details,
                    hasScreenshots = hasScreenshots,
                    isInLibrary = isInLibrary,
                    isFavorite = isFavorite,
                    libraryList = libraryList,
                    videosState = videosState,
                    isWatchLoading = isWatchLoading,
                    watchProgress = watchProgress,
                    canSubscribe = canSubscribe,
                    restoreFocusRequest = restoreButtonFocusRequest,
                    hiddenActions = hiddenActions,
                    focusOnResume = !pausedInContent,
                    firstFocusRequester = barFirstFocus,
                    bottomFocusRequester = tabsFocus,
                    lastRowEntryFocusRequester = barLastRowFocus,
                    onWatchSelected = onWatchSelected,
                    onLibraryToggle = onLibraryToggle,
                    onFavoriteToggle = onFavoriteToggle,
                    onSubscriptionsSelected = onSubscriptionsSelected,
                    onDetailsSelected = onFullDetailsSelected,
                    onEpisodesSelected = onEpisodesSelected,
                    onTrailersSelected = onTrailersSelected,
                    onSimilarSelected = onSimilarSelected,
                    onViewingOrderSelected = onViewingOrderSelected,
                    onScreenshotsSelected = onScreenshotsSelected,
                    onRatingSelected = onRatingScreenSelected,
                    onCollectionsSelected = onCollectionsSelected,
                    onCommentsSelected = onCommentsSelected,
                    onReviewsSelected = onReviewsSelected,
                    onBloggerVideosSelected = onBloggerVideosSelected,
                    onRandomAnimeSelected = onRandomAnimeSelected,
                    isRandomAnimeLoading = isRandomAnimeLoading,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Spacer(modifier = Modifier.height(SectionGap))

            val labels = DetailsMinimalTab.entries.associateWith { stringResource(it.labelRes) }
            DetailsMinimalTabRow(
                tabs = tabs,
                selected = currentTab,
                label = { labels.getValue(it) },
                onSelect = { selectedTab = it },
                tabRequester = tabRequesters::getValue,
                upRequester = barLastRowFocus,
                downRequester = contentFocus,
                modifier = Modifier.onSizeChanged { tabsHeightPx = it.height },
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(contentHeight)
                    .focusRequester(contentFocus)
                    .onFocusChanged { contentHasFocus = it.hasFocus }
                    // «Вверх» с верхнего края содержимого — на выбранную вкладку. Геометрический поиск попал бы
                    // на вкладку над карточкой и переключил бы содержимое.
                    .focusProperties {
                        // Встроенные экраны при показе сами просят фокус (первый чип, карточка). Пока пользователь
                        // на вкладках, такой вход отменяем: иначе фокус проваливается в содержимое при листании.
                        onEnter = {
                            if (requestedFocusDirection != FocusDirection.Down && !allowContentEnter[0]) {
                                cancelFocusChange()
                            }
                        }
                        onExit = {
                            if (requestedFocusDirection == FocusDirection.Up) tabsFocus.tryRequestFocus()
                        }
                    }
                    .tvFocusRestorer(),
            ) {
                key(displayedTab) {
                    DetailsMinimalTabContent(
                        tab = displayedTab,
                        state = state,
                        details = details,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

/** Шапка: постер, название, метаданные; [actions] — панель кнопок под ними. */
@Composable
private fun MinimalHero(
    details: AnimeDetails,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        val posterShape = RoundedCornerShape(16.dp)
        Card(
            modifier = Modifier
                .width(MinimalPosterWidth)
                .aspectRatio(2f / 3f)
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f), posterShape),
            shape = posterShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        ) {
            FallbackAsyncImage(
                models = details.poster.posterUrls(),
                contentDescription = details.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = details.title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            HeroMetaRow(details)
            HeroEpisodesRow(details)
            val description = details.description.trim()
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            actions()
        }
    }
}

private val DetailsMinimalTab.labelRes: Int
    get() = when (this) {
        DetailsMinimalTab.EPISODES -> R.string.details_episodes
        DetailsMinimalTab.INFO -> R.string.details_full_details
        DetailsMinimalTab.SCREENSHOTS -> R.string.details_screenshots_title
        DetailsMinimalTab.VIDEO -> R.string.details_trailers
        DetailsMinimalTab.RATING -> R.string.details_rating_button
        DetailsMinimalTab.RELATED -> R.string.details_viewing_order
        DetailsMinimalTab.SIMILAR -> R.string.details_similar
        DetailsMinimalTab.COLLECTIONS -> R.string.details_collections_button
        DetailsMinimalTab.REVIEWS -> R.string.details_reviews
        DetailsMinimalTab.COMMENTS -> R.string.details_comments
    }

private const val TAB_SHOW_DELAY_MILLIS = 250L
private val MinimalPosterWidth = 150.dp
private val HorizontalPadding = 48.dp
private val TopPadding = 16.dp
private val BottomPadding = 8.dp
private val SectionGap = 12.dp
