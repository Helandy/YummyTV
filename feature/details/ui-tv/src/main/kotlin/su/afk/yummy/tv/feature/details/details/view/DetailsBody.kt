package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.posterUrls
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.model.settings.DetailsSectionKind
import su.afk.yummy.tv.core.utils.formatting.toCompactCount
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.details.utils.focusKey
import su.afk.yummy.tv.feature.details.details.utils.isFocusRow
import su.afk.yummy.tv.feature.details.details.utils.tryRequestFocus
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.presentation.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DetailsBody(
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
) {
    val barFocusRequester = remember { FocusRequester() }
    // Нижний ряд кнопок: вход в бар снизу. Ряд прокручивается вбок, и поиск фокуса проваливался в сам
    // контейнер прокрутки — фокус пропадал до следующего нажатия.
    val barLastRowFocusRequester = remember { FocusRequester() }
    // Цель фокуса в ленте: после возврата из подэкрана фокус возвращается в неё, а не на кнопки хиро.
    var lastFocusTarget by rememberSaveable { mutableStateOf<String?>(null) }
    var restoreTarget by rememberSaveable { mutableStateOf<String?>(null) }
    val sectionRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(key: String) = sectionRequesters.getOrPut(key) { FocusRequester() }

    @Composable
    fun Modifier.sectionFocus(key: String): Modifier = this
        .focusRequester(requesterFor(key))
        .onFocusChanged { if (it.hasFocus) lastFocusTarget = key }
        .tvFocusRestorer()

    // Последняя карточка каждого ряда — вход в ряд, когда фокус приходит в него сверху или снизу.
    val lastRowIndex = remember { mutableMapOf<String, Int>() }

    // Ряды запоминают точную карточку: ключ цели — «секция:индекс».
    fun rowItemFocus(section: String): @Composable (Int) -> Modifier = { index ->
        val key = "$section:$index"
        Modifier
            .focusRequester(requesterFor(key))
            .onFocusChanged {
                if (it.isFocused) {
                    lastFocusTarget = key
                    lastRowIndex[section] = index
                }
            }
    }

    // Чипы живут внутри хиро: оно блокирует фокус на время восстановления, поэтому чип разрешает его себе сам,
    // а ключ сбрасывает при уходе, чтобы после кнопок не остался устаревший.
    // «Вверх» из описания возвращает на последний чип, а не мимо чипов к кнопкам.
    var lastChipIndex by remember { mutableIntStateOf(0) }
    val heroChipFocus: @Composable (Int) -> Modifier = { index ->
        val key = "chips:$index"
        Modifier
            .focusProperties {
                canFocus = true
                // Вверх — явно на нижний ряд кнопок: поиск проваливался в его контейнер прокрутки, и фокус пропадал.
                up = barLastRowFocusRequester
            }
            .focusRequester(requesterFor(key))
            .onFocusChanged {
                if (it.isFocused) {
                    lastChipIndex = index
                    lastFocusTarget = key
                } else if (lastFocusTarget == key) {
                    lastFocusTarget = null
                }
            }
    }

    val scope = rememberCoroutineScope()
    // Позиция ленты запоминается по мере прокрутки и применяется при создании состояния, чтобы экран сразу
    // открывался на нужном месте, а не прокручивался на глазах после восстановления фокуса.
    var savedScrollIndex by rememberSaveable { mutableIntStateOf(0) }
    var savedScrollOffset by rememberSaveable { mutableIntStateOf(0) }
    val listState = remember { LazyListState(savedScrollIndex, savedScrollOffset) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                savedScrollIndex = index
                savedScrollOffset = offset
            }
    }
    // Пока фокус не вернулся в ленту, хиро не берёт его: иначе система на миг фокусирует кнопки,
    // и лента сама прокручивается к ним, а потом дёргается обратно.
    val isRestoringFocusState = remember { mutableStateOf(restoreTarget != null) }
    var isRestoringFocus by isRestoringFocusState
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                // Цель фиксируется на паузе: после пересоздания экрана система сама ставит фокус
                // на первую карточку и успела бы затереть lastFocusTarget до возврата.
                Lifecycle.Event.ON_PAUSE -> {
                    restoreTarget = lastFocusTarget
                }
                Lifecycle.Event.ON_RESUME -> restoreTarget?.let { key ->
                    scope.launch {
                        requestFocusUntilTimeout(requesterFor(key))
                        // Прокрутку за фокусом глушим ещё пару кадров: позиция уже восстановлена.
                        repeat(3) { withFrameNanos { } }
                        isRestoringFocus = false
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val ratingOverview = state.ratingOverview
    // Студии/жанры и описание на ТВ живут в хиро: настройка блоков их только скрывает.
    val showHeroChips = DetailsSectionKind.INFO in state.sections
    val showHeroDescription = DetailsSectionKind.DESCRIPTION in state.sections
    val hasHeroChips = showHeroChips && (
        details.genres.any { it.id != null && it.title.isNotBlank() } ||
            details.studios.any { it.id != null && it.title.isNotBlank() } ||
            details.creators.any { it.id != null && it.title.isNotBlank() }
        )
    val hasScreenshots = state.screenshots.isNotEmpty()
    val hasUniverse = details.viewingOrder.size > 1
    val hasBloggerVideos = details.bloggerVideosCount > 0
    // Пока extras грузятся, кнопки этих секций скрыты, чтобы панель не прыгала, когда секция появится.
    val hiddenActions = buildSet {
        // Содержимое «Детали» целиком есть в хиро и в блоке описания.
        add(DetailsButtonAction.FULL_DETAILS)
        if (hasScreenshots) add(DetailsButtonAction.SCREENSHOTS)
        // Вселенная и коллекции есть в ленте, когда есть что показывать; пустые экраны по кнопке не нужны.
        add(DetailsButtonAction.VIEWING_ORDER)
        add(DetailsButtonAction.TRAILERS)
        add(DetailsButtonAction.RATING)
        add(DetailsButtonAction.COLLECTIONS)
        add(DetailsButtonAction.BLOGGER_VIDEOS)
        // Похожее и комментарии есть в ленте всегда; без рецензий кнопка вела бы на пустой экран.
        add(DetailsButtonAction.SIMILAR)
        add(DetailsButtonAction.COMMENTS)
        add(DetailsButtonAction.REVIEWS)
    }

    // Разделы ленты под хиро в порядке из настроек — только те, что сейчас есть что показать.
    val feedSections = state.sections.filter { section ->
        when (section) {
            // Показываются внутри хиро.
            DetailsSectionKind.INFO, DetailsSectionKind.DESCRIPTION -> false
            DetailsSectionKind.SCREENSHOTS -> hasScreenshots
            DetailsSectionKind.VIDEO -> state.trailers.isNotEmpty() || hasBloggerVideos
            // Пока extras грузятся, секции нет, чтобы лента не прыгала; после — есть всегда, даже без оценок.
            DetailsSectionKind.RATING -> !state.isExtrasLoading || ratingOverview != null
            DetailsSectionKind.UNIVERSE -> hasUniverse
            DetailsSectionKind.SIMILAR -> true
            DetailsSectionKind.COLLECTIONS -> state.collections.isNotEmpty()
            DetailsSectionKind.REVIEWS -> details.reviewsCount > 0
            DetailsSectionKind.COMMENTS -> true
        }
    }

    // Вход в блок ленты при переходе вверх/вниз. Геометрический поиск между блоками ненадёжен: из ряда
    // под хиро он проскакивал чипы к кнопкам, а в ряд попадал на ближайшую по горизонтали карточку,
    // и с переставленными или скрытыми разделами это всплывало в разных местах. Не удалось — обычный поиск.
    fun enterHeroFromBelow(): Boolean = listOfNotNull(
        requesterFor(DESCRIPTION_TAIL_FOCUS_KEY),
        requesterFor(DESCRIPTION_HEAD_FOCUS_KEY),
        if (hasHeroChips) requesterFor("chips:$lastChipIndex") else null,
        if (state.showSectionsHint) requesterFor(HERO_HINT_FOCUS_KEY) else null,
        barLastRowFocusRequester,
        barFocusRequester,
    ).any { it.tryRequestFocus() }

    fun enterSection(section: DetailsSectionKind): Boolean {
        val key = section.focusKey
        val requesters = if (section.isFocusRow) {
            listOf(requesterFor("$key:${lastRowIndex[key] ?: 0}"), requesterFor("$key:0"))
        } else {
            listOf(requesterFor(key))
        }
        return requesters.distinct().any { it.tryRequestFocus() }
    }

    // [position] — место блока в ленте: -1 хиро, дальше индексы [feedSections].
    fun Modifier.feedFocusNeighbors(position: Int): Modifier = this
        .focusProperties {
            onExit = {
                val target = when (requestedFocusDirection) {
                    FocusDirection.Up -> position - 1
                    FocusDirection.Down -> position + 1
                    else -> null
                }
                when {
                    target == null -> Unit
                    target == -1 -> enterHeroFromBelow()
                    target in feedSections.indices -> enterSection(feedSections[target])
                }
            }
        }
        .focusGroup()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        // Хиро чуть ниже экрана: заголовок первой секции выглядывает снизу и подсказывает, что лента есть.
        val heroHeight = maxHeight - HeroPeekHeight
        val defaultSpec = LocalBringIntoViewSpec.current
        val restoringSpec = remember(defaultSpec) {
            object : BringIntoViewSpec {
                override val scrollAnimationSpec = defaultSpec.scrollAnimationSpec
                override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
                    if (isRestoringFocusState.value) 0f else defaultSpec.calculateScrollDistance(offset, size, containerSize)
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides restoringSpec) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                item(key = "hero") {
                    DetailsHero(
                        modifier = Modifier
                            .feedFocusNeighbors(position = -1)
                            .focusProperties { canFocus = !isRestoringFocus }
                            .onFocusChanged {
                                if (it.hasFocus && lastFocusTarget?.startsWith("chips:") != true &&
                                    lastFocusTarget?.startsWith("description:") != true &&
                                    lastFocusTarget != HERO_HINT_FOCUS_KEY
                                ) {
                                    lastFocusTarget = null
                                }
                            },
                        details = details,
                        hasScreenshots = hasScreenshots,
                        minHeight = heroHeight,
                        hintFocusRequester = requesterFor(HERO_HINT_FOCUS_KEY),
                        // Без ключа уход с чипа обнулял бы цель, хиро считало бы, что фокус потерян, и возвращало его на кнопки.
                        onHintFocused = { focused ->
                            if (focused) {
                                lastFocusTarget = HERO_HINT_FOCUS_KEY
                            } else if (lastFocusTarget == HERO_HINT_FOCUS_KEY) {
                                lastFocusTarget = null
                            }
                        },
                        downFocusRequester = barFocusRequester,
                        isInLibrary = isInLibrary,
                        isFavorite = isFavorite,
                        libraryList = libraryList,
                        userRating = userRating,
                        videosState = videosState,
                        isWatchLoading = isWatchLoading,
                        watchProgress = watchProgress,
                        canSubscribe = canSubscribe,
                        restoreButtonFocusRequest = restoreButtonFocusRequest,
                        hiddenActions = hiddenActions,
                        // restoreTarget держит цель, пока система не успела отдать фокус хиро и стереть lastFocusTarget.
                        focusOnResume = lastFocusTarget == null && restoreTarget == null,
                        showSectionsHint = state.showSectionsHint,
                        onSectionsHintDismissed = { onEvent(DetailsState.Event.SectionsHintDismissed) },
                        onWatchSelected = onWatchSelected,
                        onLibraryToggle = onLibraryToggle,
                        onFavoriteToggle = onFavoriteToggle,
                        onSubscriptionsSelected = onSubscriptionsSelected,
                        onFullDetailsSelected = onFullDetailsSelected,
                        onEpisodesSelected = onEpisodesSelected,
                        onTrailersSelected = onTrailersSelected,
                        onSimilarSelected = onSimilarSelected,
                        onViewingOrderSelected = onViewingOrderSelected,
                        onScreenshotsSelected = onScreenshotsSelected,
                        onRatingScreenSelected = onRatingScreenSelected,
                        onCollectionsSelected = onCollectionsSelected,
                        onCommentsSelected = onCommentsSelected,
                        onReviewsSelected = onReviewsSelected,
                        onBloggerVideosSelected = onBloggerVideosSelected,
                        onGenreSelected = { onEvent(DetailsState.Event.GenreSelected(it)) },
                        onStudioSelected = { id, url -> onEvent(DetailsState.Event.StudioSelected(id, url)) },
                        onDirectorSelected = { onEvent(DetailsState.Event.DirectorSelected(it)) },
                        chipFocusModifier = heroChipFocus,
                        chipsEntryFocusRequester = if (hasHeroChips) requesterFor("chips:0") else null,
                        barLastRowFocusRequester = barLastRowFocusRequester,
                        // Описание внутри хиро: как и чип, само разрешает себе фокус на время восстановления.
                        descriptionFocusModifier = Modifier
                            .focusProperties {
                                canFocus = true
                                up = if (hasHeroChips) requesterFor("chips:$lastChipIndex") else barLastRowFocusRequester
                            }
                            .then(rowItemFocus(DESCRIPTION_ROW)(DESCRIPTION_HEAD_INDEX)),
                        descriptionTailFocusModifier = Modifier
                            .focusProperties { canFocus = true }
                            .then(rowItemFocus(DESCRIPTION_ROW)(DESCRIPTION_TAIL_INDEX)),
                        showChips = showHeroChips,
                        showDescription = showHeroDescription,
                    )
                }
                feedSections.forEachIndexed { position, section ->
                    val neighbors = Modifier.feedFocusNeighbors(position)
                    when (section) {
                        DetailsSectionKind.INFO, DetailsSectionKind.DESCRIPTION -> Unit
                        DetailsSectionKind.SCREENSHOTS -> item(key = "screenshots") {
                            DetailsScreenshotsRow(
                                screenshots = state.screenshots,
                                onScreenshotClick = { onEvent(DetailsState.Event.ScreenshotSelected(it)) },
                                modifier = neighbors.tvWholeItemBringIntoView(),
                                itemFocusModifier = rowItemFocus(section.focusKey),
                            )
                        }
                        DetailsSectionKind.VIDEO -> item(key = "video") {
                            DetailsVideoSection(
                                trailers = state.trailers,
                                bloggerVideosImageModels = details.poster.posterUrls(),
                                hasBloggerVideos = hasBloggerVideos,
                                onTrailersClick = onTrailersSelected,
                                onBloggerVideosClick = onBloggerVideosSelected,
                                modifier = neighbors.tvWholeItemBringIntoView(),
                                itemFocusModifier = rowItemFocus(section.focusKey),
                            )
                        }
                        DetailsSectionKind.RATING -> item(key = "rating") {
                            DetailsRatingOverviewSection(
                                overview = ratingOverview,
                                userRating = userRating,
                                onClick = onRatingScreenSelected,
                                modifier = neighbors.sectionFocus(section.focusKey).tvWholeItemBringIntoView(),
                            )
                        }
                        DetailsSectionKind.UNIVERSE -> item(key = "universe") {
                            DetailsUniverseRow(
                                items = details.viewingOrder,
                                currentAnimeId = details.id,
                                onAnimeClick = { onEvent(DetailsState.Event.AnimeSelected(it)) },
                                onShowAllClick = onViewingOrderSelected,
                                modifier = neighbors.tvWholeItemBringIntoView(),
                                itemFocusModifier = rowItemFocus(section.focusKey),
                            )
                        }
                        DetailsSectionKind.SIMILAR -> item(key = "similar") {
                            DetailsLinkRow(
                                label = stringResource(R.string.details_mobile_recommended_title),
                                icon = Icons.Filled.AutoAwesome,
                                onClick = onSimilarSelected,
                                modifier = neighbors.sectionFocus(section.focusKey).tvWholeItemBringIntoView(),
                            )
                        }
                        DetailsSectionKind.COLLECTIONS -> item(key = "collections") {
                            DetailsCollectionsRow(
                                collections = state.collections,
                                onCollectionClick = { onEvent(DetailsState.Event.CollectionSelected(it)) },
                                onShowAllClick = onCollectionsSelected,
                                modifier = neighbors.tvWholeItemBringIntoView(),
                                itemFocusModifier = rowItemFocus(section.focusKey),
                            )
                        }
                        DetailsSectionKind.REVIEWS -> item(key = "reviews") {
                            DetailsLinkRow(
                                label = stringResource(R.string.details_reviews),
                                icon = Icons.Filled.RateReview,
                                badge = details.reviewsCount.toCompactCount(),
                                onClick = onReviewsSelected,
                                modifier = neighbors.sectionFocus(section.focusKey).tvWholeItemBringIntoView(),
                            )
                        }
                        DetailsSectionKind.COMMENTS -> item(key = "comments") {
                            DetailsCommentsRow(
                                commentsBadge = details.commentsCount.takeIf { it > 0 }?.toCompactCount(),
                                onCommentsClick = onCommentsSelected,
                                onSearchClick = { onEvent(DetailsState.Event.NewSearchSelected) },
                                modifier = neighbors.sectionFocus(section.focusKey).tvWholeItemBringIntoView(),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val HeroPeekHeight = 96.dp
private const val HERO_HINT_FOCUS_KEY = "hint"
private const val DESCRIPTION_ROW = "description"
private const val DESCRIPTION_HEAD_INDEX = -1
private const val DESCRIPTION_TAIL_INDEX = -2
private const val DESCRIPTION_HEAD_FOCUS_KEY = "$DESCRIPTION_ROW:$DESCRIPTION_HEAD_INDEX"
private const val DESCRIPTION_TAIL_FOCUS_KEY = "$DESCRIPTION_ROW:$DESCRIPTION_TAIL_INDEX"
