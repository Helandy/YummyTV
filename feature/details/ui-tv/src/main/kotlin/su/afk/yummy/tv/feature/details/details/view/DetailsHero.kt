package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.components.MarqueeTitleText
import su.afk.yummy.tv.core.designsystem.presenter.components.toRatingColor
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.R
import su.afk.yummy.tv.feature.details.details.DetailsWatchProgressIndex
import su.afk.yummy.tv.feature.details.details.VideosUiState
import su.afk.yummy.tv.feature.details.utils.bestUrl
import su.afk.yummy.tv.feature.details.utils.formatAiredProgress
import su.afk.yummy.tv.feature.details.utils.formatRating
import su.afk.yummy.tv.feature.details.utils.formatViews

@Composable
internal fun DetailsHero(
    details: AnimeDetails,
    downFocusRequester: FocusRequester,
    isInLibrary: Boolean,
    isFavorite: Boolean,
    libraryList: UserAnimeList?,
    videosState: VideosUiState,
    isWatchLoading: Boolean,
    watchProgress: DetailsWatchProgressIndex,
    canSubscribe: Boolean,
    detailsButtonOrder: List<DetailsButtonAction>,
    restoreButtonFocusRequest: Int,
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HeroBackdrop(details)

        val isCompactHeight = maxHeight < 560.dp
        val horizontalPadding = 20.dp
        val verticalPadding = when {
            isCompactHeight -> 20.dp
            else -> 32.dp
        }
        val rowSpacing = if (isCompactHeight) 28.dp else 48.dp
        val columnSpacing = if (isCompactHeight) 8.dp else 12.dp
        val titleMaxLines = 3
        val descriptionMaxLines = 3
        val posterWidth = if (isCompactHeight) 260.dp else 300.dp
        val buttonBarHeight = if (isCompactHeight) 132.dp else 150.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
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
                MarqueeTitleText(
                    text = details.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = 30.sp,
                        color = if (titleFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    minLines = 1,
                    maxLines = titleMaxLines,
                    isFocused = titleFocused,
                    marqueeWhenOverflow = true,
                    modifier = Modifier
                        .focusRequester(titleFocusRequester)
                        .focusProperties { down = downFocusRequester }
                        .focusable()
                        .onFocusChanged { fs -> titleFocused = fs.isFocused },
                )

                HeroMetaRow(details)
                HeroGenresRow(details)
                HeroEpisodesRow(details)

                if (details.description.isNotBlank()) {
                    Text(
                        text = details.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                        minLines = descriptionMaxLines,
                        maxLines = descriptionMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                DetailsButtonBar(
                    details = details,
                    isInLibrary = isInLibrary,
                    isFavorite = isFavorite,
                    libraryList = libraryList,
                    videosState = videosState,
                    isWatchLoading = isWatchLoading,
                    watchProgress = watchProgress,
                    canSubscribe = canSubscribe,
                    buttonOrder = detailsButtonOrder,
                    restoreFocusRequest = restoreButtonFocusRequest,
                    firstFocusRequester = downFocusRequester,
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
                    modifier = Modifier.padding(top = 8.dp),
                    height = buttonBarHeight,
                )
            }

            Column(
                modifier = Modifier
                    .width(posterWidth)
                    .graphicsLayer { alpha = posterAlpha },
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                HeroRatingRow(details)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    AsyncImage(
                        model = details.poster?.bestUrl,
                        contentDescription = details.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

/**
 * Blurred, cropped poster behind the hero content, dimmed by a scrim so the
 * text stays readable. On devices without RenderEffect support (API < 31)
 * blur is a no-op — the scrim alone keeps contrast.
 */
@Composable
private fun HeroBackdrop(details: AnimeDetails) {
    val backdropUrl = details.poster?.run { big ?: medium ?: small } ?: return
    val surface = MaterialTheme.colorScheme.surface
    AsyncImage(
        model = backdropUrl,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = 0.38f,
        modifier = Modifier
            .fillMaxSize()
            .blur(HERO_BACKDROP_BLUR),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        surface.copy(alpha = 0.55f),
                        surface.copy(alpha = 0.85f),
                    ),
                ),
            ),
    )
}

private val HERO_BACKDROP_BLUR = 26.dp
private val HERO_APPEAR_SHIFT = 18.dp
private const val HERO_APPEAR_MILLIS = 380
private const val HERO_POSTER_APPEAR_DELAY_MILLIS = 100

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroRatingRow(details: AnimeDetails) {
    val yaniRating = details.rating.average
    val ratings = buildList {
        details.rating.kinopoisk?.let {
            add(
                ExternalRatingLabel(
                    label = stringResource(
                        R.string.details_kinopoisk_rating,
                        it.formatRating(),
                    ),
                    rating = it,
                )
            )
        }
        details.rating.shikimori?.let {
            add(
                ExternalRatingLabel(
                    label = stringResource(
                        R.string.details_shikimori_rating,
                        it.formatRating(),
                    ),
                    rating = it,
                )
            )
        }
        details.rating.myAnimeList?.let {
            add(
                ExternalRatingLabel(
                    label = stringResource(
                        R.string.details_mal_rating,
                        it.formatRating(),
                    ),
                    rating = it,
                )
            )
        }
    }
    if (yaniRating == null && ratings.isEmpty()) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        yaniRating?.let { rating ->
            YaniRatingLabel(rating)
        }
        ratings.forEach { rating ->
            RatingLabel(rating)
        }
    }
}

private data class ExternalRatingLabel(
    val label: String,
    val rating: Double,
)

@Composable
private fun RatingLabel(rating: ExternalRatingLabel) {
    Text(
        text = rating.label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = rating.rating.toRatingColor(),
    )
}

@Composable
private fun YaniRatingLabel(rating: Double) {
    val color = rating.toRatingColor()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = color,
            modifier = Modifier
                .height(13.dp)
                .width(13.dp),
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = rating.formatRating(),
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = color,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroMetaRow(details: AnimeDetails) {
    val items = buildList {
        details.year?.let { add(it.toString()) }
        details.type?.let { add(it) }
        details.status?.let { add(it) }
        details.ageRating?.let { add(it) }
    }
    val viewsLabel = details.views?.formatViews()
    if (items.isEmpty() && viewsLabel == null) return
    val chipColor = MaterialTheme.colorScheme.onSurface
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items.forEach { label ->
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = chipColor.copy(alpha = 0.85f),
                modifier = Modifier
                    .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
        }
        viewsLabel?.let { views ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = chipColor.copy(alpha = 0.85f),
                    modifier = Modifier
                        .height(10.dp)
                        .width(10.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = views,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = chipColor.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun HeroGenresRow(details: AnimeDetails) {
    val genres = details.genres.take(5).joinToString(" • ") { it.title }
    if (genres.isBlank()) return
    Text(
        text = genres,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun HeroEpisodesRow(details: AnimeDetails) {
    val episodeProgress = details.episodes?.formatAiredProgress(details.status) ?: return
    Text(
        text = episodeProgress,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
    )
}
