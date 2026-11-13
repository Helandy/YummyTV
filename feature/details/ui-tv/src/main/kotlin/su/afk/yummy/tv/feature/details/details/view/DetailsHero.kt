package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import su.afk.yummy.tv.core.designsystem.components.FallbackAsyncImage
import su.afk.yummy.tv.core.designsystem.components.MarqueeTitleText
import su.afk.yummy.tv.core.designsystem.components.UserRatingChip
import su.afk.yummy.tv.core.designsystem.focus.tvWholeItemBringIntoView
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.posterUrls
import su.afk.yummy.tv.core.model.settings.DetailsButtonAction
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.model.DetailsWatchProgressIndex

@Composable
internal fun DetailsHero(
    details: AnimeDetails,
    hasScreenshots: Boolean,
    modifier: Modifier = Modifier,
    downFocusRequester: FocusRequester,
    isInLibrary: Boolean,
    isFavorite: Boolean,
    libraryList: UserAnimeList?,
    userRating: Int?,
    videosState: VideosUiState,
    isWatchLoading: Boolean,
    watchProgress: DetailsWatchProgressIndex,
    canSubscribe: Boolean,
    restoreButtonFocusRequest: Int,
    hiddenActions: Set<DetailsButtonAction>,
    focusOnResume: Boolean,
    showSectionsHint: Boolean,
    onSectionsHintDismissed: () -> Unit,
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
    onGenreSelected: (Int) -> Unit,
    onStudioSelected: (id: Int, url: String?) -> Unit,
    onDirectorSelected: (Int) -> Unit,
    chipFocusModifier: @Composable (index: Int) -> Modifier,
    chipsEntryFocusRequester: FocusRequester?,
    hintFocusRequester: FocusRequester,
    onHintFocused: (Boolean) -> Unit,
    minHeight: Dp,
    descriptionFocusModifier: Modifier,
    descriptionTailFocusModifier: Modifier,
) {
    val titleFocusRequester = remember { FocusRequester() }

    var titleFocused by remember { mutableStateOf(false) }

    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val infoAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = HERO_APPEAR_MILLIS),
        label = "heroInfoAlpha",
    )
    val infoShift by animateFloatAsState(
        targetValue = if (appeared) 0f else 1f,
        animationSpec = tween(durationMillis = HERO_APPEAR_MILLIS),
        label = "heroInfoShift",
    )
    val posterAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(
            durationMillis = HERO_APPEAR_MILLIS,
            delayMillis = HERO_POSTER_APPEAR_DELAY_MILLIS,
        ),
        label = "heroPosterAlpha",
    )
    val density = LocalDensity.current
    val infoShiftPx = with(density) { HERO_APPEAR_SHIFT.toPx() }

    // Высота хиро — минимум: если контент (длинное название, чипы, подсказка) не влезает, хиро растёт, а не наезжает на описание.
    var heroTop by remember { mutableFloatStateOf(0f) }
    var descriptionTop by remember { mutableFloatStateOf(0f) }
    Box(
        modifier = modifier
            .onGloballyPositioned { heroTop = it.positionInRoot().y }
            // Фокус на описании поднимает ленту так, чтобы оно уместилось вместе с продолжением под постером;
            // для остального хиро (кнопки, чипы) подгоняется весь блок.
            .tvWholeItemBringIntoView(rectOverride = { localRect, size ->
                val descriptionLocalTop = descriptionTop - heroTop
                if (size != IntSize.Zero && descriptionTop > 0f && localRect.top >= descriptionLocalTop - 40f) {
                    Rect(0f, localRect.top, size.width.toFloat(), size.height.toFloat())
                } else {
                    null
                }
            })
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Box(modifier = Modifier.matchParentSize()) { HeroBackdrop(details) }

        val isCompactHeight = minHeight < 560.dp
        val horizontalPadding = 20.dp
        val verticalPadding = when {
            isCompactHeight -> 20.dp
            else -> 32.dp
        }
        val rowSpacing = if (isCompactHeight) 28.dp else 48.dp
        val columnSpacing = if (isCompactHeight) 8.dp else 12.dp
        val titleMaxLines = 3
        val posterWidth = if (isCompactHeight) HERO_POSTER_WIDTH_COMPACT else HERO_POSTER_WIDTH

        val description = details.description.trim()
        val textMeasurer = rememberTextMeasurer()
        val descriptionStyle = MaterialTheme.typography.bodyLarge
        var descriptionExpanded by rememberSaveable { mutableStateOf(false) }
        var tailOverflows by remember { mutableStateOf(false) }
        var posterBottom by remember { mutableFloatStateOf(0f) }
        var headSize by remember { mutableStateOf(IntSize.Zero) }
        val paddingPx = with(density) { DetailsDescriptionPadding.toPx() }
        // Описание «обтекает» постер: сколько строк влезает рядом с ним, столько в голове, остальное — под постером.
        val splitIndex = remember(description, descriptionStyle, headSize, posterBottom, descriptionTop) {
            val availableHeight = posterBottom - descriptionTop - 2 * paddingPx
            val textWidth = (headSize.width - 2 * paddingPx).toInt()
            if (description.isEmpty() || textWidth <= 0 || availableHeight <= 0f) {
                description.length
            } else {
                val layout = textMeasurer.measure(
                    text = description,
                    style = descriptionStyle,
                    constraints = Constraints(maxWidth = textWidth),
                )
                val fitting = (0 until layout.lineCount).count { layout.getLineBottom(it) <= availableHeight }
                when {
                    fitting <= 0 || fitting >= layout.lineCount -> description.length
                    else -> layout.getLineEnd(fitting - 1)
                }
            }
        }
        val descriptionHead = description.substring(0, splitIndex).trimEnd()
        val descriptionTail = description.substring(splitIndex).trimStart()

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        alpha = infoAlpha
                        translationY = infoShift * infoShiftPx
                    },
                verticalArrangement = Arrangement.spacedBy(columnSpacing),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    MarqueeTitleText(
                        text = details.title,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 30.sp,
                            color = if (titleFocused) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface,
                        ),
                        fontWeight = FontWeight.SemiBold,
                        minLines = 1,
                        maxLines = titleMaxLines,
                        isFocused = titleFocused,
                        marqueeWhenOverflow = true,
                        modifier = Modifier
                            .weight(1f)
                            .alignByBaseline()
                            .focusRequester(titleFocusRequester)
                            .focusProperties { down = downFocusRequester }
                            .focusable()
                            .onFocusChanged { fs -> titleFocused = fs.isFocused },
                    )
                    userRating?.let { userRating ->
                        // По первой строке заголовка, а не по центру двух строк.
                        UserRatingChip(
                            rating = userRating,
                            textStyle = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.alignByBaseline(),
                        )
                    }
                }

                HeroMetaRow(details)
                HeroEpisodesRow(details)

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
                    focusOnResume = focusOnResume,
                    firstFocusRequester = downFocusRequester,
                    bottomFocusRequester = chipsEntryFocusRequester,
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
                    modifier = Modifier.padding(top = 8.dp),
                )

                HeroChipsRow(
                    details = details,
                    onGenreSelected = onGenreSelected,
                    onStudioSelected = onStudioSelected,
                    onDirectorSelected = onDirectorSelected,
                    // Кнопка подсказки стоит справа, а чип слева: поиск фокуса вниз по геометрии её не находит.
                    chipFocusModifier = { index ->
                        chipFocusModifier(index).then(
                            if (showSectionsHint) {
                                Modifier.focusProperties { down = hintFocusRequester }
                            } else {
                                Modifier
                            },
                        )
                    },
                )

                if (showSectionsHint) {
                    DetailsSectionsHint(
                        onDismiss = onSectionsHintDismissed,
                        dismissFocusModifier = Modifier
                            .focusRequester(hintFocusRequester)
                            .onFocusChanged { onHintFocused(it.isFocused) },
                    )
                }

                // Описание занимает свободное место под кнопками рядом с постером; по OK раскрывается.
                if (descriptionHead.isNotEmpty()) {
                    DetailsDescriptionHead(
                        text = descriptionHead,
                        onToggle = { descriptionExpanded = !descriptionExpanded },
                        modifier = descriptionFocusModifier
                            .onSizeChanged { headSize = it }
                            .onGloballyPositioned { descriptionTop = it.positionInRoot().y },
                    )
                }
            }

            Column(
                modifier = Modifier
                    .width(posterWidth)
                    .onGloballyPositioned { posterBottom = it.positionInRoot().y + it.size.height }
                    .graphicsLayer { alpha = posterAlpha },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeroRatingRow(details)

                val posterShape = RoundedCornerShape(HERO_POSTER_CORNER)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .shadow(HERO_POSTER_ELEVATION, posterShape)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f),
                            posterShape,
                        ),
                    shape = posterShape,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    FallbackAsyncImage(
                        models = details.poster.posterUrls(),
                        contentDescription = details.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        if (descriptionTail.isNotEmpty()) {
            DetailsDescriptionTail(
                text = descriptionTail,
                expanded = descriptionExpanded,
                overflows = tailOverflows,
                onOverflowChanged = { tailOverflows = it },
                onToggle = { descriptionExpanded = !descriptionExpanded },
                modifier = descriptionTailFocusModifier,
            )
        }
        }
    }
}

internal val HERO_BACKDROP_BLUR = 26.dp
private val HERO_POSTER_WIDTH = 340.dp
private val HERO_POSTER_WIDTH_COMPACT = 300.dp
private val HERO_POSTER_CORNER = 16.dp
private val HERO_POSTER_ELEVATION = 16.dp
internal val HERO_APPEAR_SHIFT = 18.dp
internal const val HERO_APPEAR_MILLIS = 380
internal const val HERO_POSTER_APPEAR_DELAY_MILLIS = 100
