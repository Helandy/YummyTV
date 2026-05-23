package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.designsystem.presenter.components.MarqueeTitleText
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressEntry
import su.afk.yummy.tv.domain.anime.AnimeDetails
import su.afk.yummy.tv.feature.details.R

@Composable
internal fun DetailsHero(
    details: AnimeDetails,
    downFocusRequester: FocusRequester,
    isInLibrary: Boolean,
    isWatchLoading: Boolean,
    watchProgress: Map<String, WatchProgressEntry>,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFullDetailsSelected: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
) {
    val titleFocusRequester = remember { FocusRequester() }

    var titleFocused by remember { mutableStateOf(false) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
    ) {
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
        val buttonBarHeight = if (isCompactHeight) 124.dp else 150.dp

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
            horizontalArrangement = Arrangement.spacedBy(rowSpacing),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(columnSpacing),
            ) {
                MarqueeTitleText(
                    text = details.title,
                    style = MaterialTheme.typography.displaySmall.copy(
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
                        maxLines = descriptionMaxLines,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                DetailsButtonBar(
                    details = details,
                    isInLibrary = isInLibrary,
                    isWatchLoading = isWatchLoading,
                    watchProgress = watchProgress,
                    firstFocusRequester = downFocusRequester,
                    onWatchSelected = onWatchSelected,
                    onLibraryToggle = onLibraryToggle,
                    onDetailsSelected = onFullDetailsSelected,
                    onEpisodesSelected = onEpisodesSelected,
                    onTrailersSelected = onTrailersSelected,
                    onSimilarSelected = onSimilarSelected,
                    onViewingOrderSelected = onViewingOrderSelected,
                    onScreenshotsSelected = onScreenshotsSelected,
                    modifier = Modifier.padding(top = 8.dp),
                    height = buttonBarHeight,
                )
            }

            Column(
                modifier = Modifier.width(posterWidth),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroRatingRow(details: AnimeDetails) {
    val yaniRating = details.rating.average
    val ratings = buildList {
        details.rating.kinopoisk?.let { add(stringResource(R.string.details_kinopoisk_rating, it.formatRating())) }
        details.rating.shikimori?.let { add("Shikimori ${it.formatRating()}") }
        details.rating.myAnimeList?.let { add("MAL ${it.formatRating()}") }
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
        ratings.forEach { label ->
            RatingLabel(label)
        }
    }
}

@Composable
private fun RatingLabel(label: String) {
    Text(
        text = label,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
    )
}

@Composable
private fun YaniRatingLabel(rating: Double) {
    val color = rating.toYaniRatingColor()
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
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier
                    .background(chipColor.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = chipColor.copy(alpha = 0.85f),
                    modifier = Modifier.height(10.dp).width(10.dp),
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
    val episodeProgress = details.episodes?.formatAiredProgress() ?: return
    Text(
        text = episodeProgress,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
    )
}

private fun Double.toYaniRatingColor(): Color = when {
    this < 7.0 -> Color(0xFFE53935)
    this <= 9.0 -> Color(0xFFFFC857)
    else -> Color(0xFF69F0AE)
}
