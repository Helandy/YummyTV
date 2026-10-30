package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import su.afk.yummy.tv.core.designsystem.locals.LocalPosterQuality
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.urlsByQuality
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.core.utils.formatting.toCompactCount
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
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

    // Ряды запоминают точную карточку: ключ цели — «секция:индекс».
    fun rowItemFocus(section: String): @Composable (Int) -> Modifier = { index ->
        val key = "$section:$index"
        Modifier
            .focusRequester(requesterFor(key))
            .onFocusChanged { if (it.isFocused) lastFocusTarget = key }
    }

    // Чипы живут внутри хиро: оно блокирует фокус на время восстановления, поэтому чип разрешает его себе сам,
    // а ключ сбрасывает при уходе, чтобы после кнопок не остался устаревший.
    // «Вверх» из описания возвращает на последний чип, а не мимо чипов к кнопкам.
    var lastChipIndex by remember { mutableIntStateOf(0) }
    val heroChipFocus: @Composable (Int) -> Modifier = { index ->
        val key = "chips:$index"
        Modifier
            .focusProperties { canFocus = true }
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
    val hasHeroChips = details.genres.any { it.id != null && it.title.isNotBlank() } ||
        details.studios.any { it.id != null && it.title.isNotBlank() } ||
        details.creators.any { it.id != null && it.title.isNotBlank() }
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
                            .focusProperties { canFocus = !isRestoringFocus }
                            .onFocusChanged {
                                if (it.hasFocus && lastFocusTarget?.startsWith("chips:") != true && lastFocusTarget != HERO_HINT_FOCUS_KEY) {
                                    lastFocusTarget = null
                                }
                            }
                            .tvWholeItemBringIntoView(),
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
                    )
                }
                item(key = "description") {
                    DetailsDescriptionSection(
                        details = details,
                        descriptionFocusModifier = rowItemFocus("description")(-1).then(
                            if (hasHeroChips) {
                                Modifier.focusProperties { up = requesterFor("chips:$lastChipIndex") }
                            } else {
                                Modifier
                            },
                        ),
                        modifier = Modifier
                            .tvWholeItemBringIntoView()
                            .padding(horizontal = DetailsSectionHorizontalPadding),
                    )
                }
                if (hasScreenshots) {
                    item(key = "screenshots") {
                        DetailsScreenshotsRow(
                            screenshots = state.screenshots,
                            onScreenshotClick = { onEvent(DetailsState.Event.ScreenshotSelected(it)) },
                            modifier = Modifier.tvWholeItemBringIntoView(),
                            itemFocusModifier = rowItemFocus("screenshots"),
                        )
                    }
                }
                if (state.trailers.isNotEmpty() || hasBloggerVideos) {
                    item(key = "video") {
                        DetailsVideoSection(
                            trailers = state.trailers,
                            bloggerVideosImageModels = details.poster.urlsByQuality(LocalPosterQuality.current),
                            hasBloggerVideos = hasBloggerVideos,
                            onTrailersClick = onTrailersSelected,
                            onBloggerVideosClick = onBloggerVideosSelected,
                            modifier = Modifier.tvWholeItemBringIntoView(),
                            itemFocusModifier = rowItemFocus("video"),
                        )
                    }
                }
                // Пока extras грузятся, секции нет, чтобы лента не прыгала; после — есть всегда, даже без оценок.
                if (!state.isExtrasLoading || ratingOverview != null) {
                    item(key = "rating") {
                        DetailsRatingOverviewSection(
                            overview = ratingOverview,
                            userRating = userRating,
                            onClick = onRatingScreenSelected,
                            modifier = Modifier.sectionFocus("rating").tvWholeItemBringIntoView(),
                        )
                    }
                }
                if (hasUniverse) {
                    item(key = "universe") {
                        DetailsUniverseRow(
                            items = details.viewingOrder,
                            currentAnimeId = details.id,
                            onAnimeClick = { onEvent(DetailsState.Event.AnimeSelected(it)) },
                            onShowAllClick = onViewingOrderSelected,
                            modifier = Modifier.tvWholeItemBringIntoView(),
                            itemFocusModifier = rowItemFocus("universe"),
                        )
                    }
                }
                item(key = "similar") {
                    DetailsLinkRow(
                        label = stringResource(R.string.details_mobile_recommended_title),
                        icon = Icons.Filled.AutoAwesome,
                        onClick = onSimilarSelected,
                        modifier = Modifier.sectionFocus("similar").tvWholeItemBringIntoView(),
                    )
                }
                if (state.collections.isNotEmpty()) {
                    item(key = "collections") {
                        DetailsCollectionsRow(
                            collections = state.collections,
                            onCollectionClick = { onEvent(DetailsState.Event.CollectionSelected(it)) },
                            onShowAllClick = onCollectionsSelected,
                            modifier = Modifier.tvWholeItemBringIntoView(),
                            itemFocusModifier = rowItemFocus("collections"),
                        )
                    }
                }
                if (details.reviewsCount > 0) {
                    item(key = "reviews") {
                        DetailsLinkRow(
                            label = stringResource(R.string.details_reviews),
                            icon = Icons.Filled.RateReview,
                            badge = details.reviewsCount.toCompactCount(),
                            onClick = onReviewsSelected,
                            modifier = Modifier.sectionFocus("reviews").tvWholeItemBringIntoView(),
                        )
                    }
                }
                item(key = "comments") {
                    DetailsLinkRow(
                        label = stringResource(R.string.details_comments),
                        icon = Icons.AutoMirrored.Filled.Comment,
                        badge = details.commentsCount.takeIf { it > 0 }?.toCompactCount(),
                        onClick = onCommentsSelected,
                        modifier = Modifier.sectionFocus("comments").tvWholeItemBringIntoView(),
                    )
                }
            }
        }
    }
}

private val HeroPeekHeight = 96.dp
private const val HERO_HINT_FOCUS_KEY = "hint"
