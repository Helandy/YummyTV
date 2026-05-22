package su.afk.yummy.tv.feature.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    watchProgress: Map<String, WatchProgressEntry>,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onEpisodesSelected: () -> Unit,
    onTrailersSelected: () -> Unit,
    onSimilarSelected: () -> Unit,
    onViewingOrderSelected: () -> Unit,
    onScreenshotsSelected: () -> Unit,
) {
    val titleFocusRequester = remember { FocusRequester() }

    var titleFocused by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(48.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                HeroRatingRow(details)

                MarqueeTitleText(
                    text = details.title,
                    style = MaterialTheme.typography.displaySmall.copy(
                        color = if (titleFocused) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    minLines = 1,
                    maxLines = 3,
                    isFocused = titleFocused,
                    modifier = Modifier
                        .focusRequester(titleFocusRequester)
                        .focusProperties { down = downFocusRequester }
                        .focusable()
                        .onFocusChanged { fs -> titleFocused = fs.isFocused },
                )

                if (details.otherTitles.isNotEmpty()) {
                    Text(
                        text = details.otherTitles.take(3).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HeroMetaRow(details)

                if (details.genres.isNotEmpty()) {
                    Text(
                        text = details.genres.take(4).joinToString(" · ") { it.title },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.60f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                details.episodes?.let { ep ->
                    val line = buildList {
                        ep.aired?.let { add(stringResource(R.string.details_aired, it)) }
                        ep.count?.takeIf { it > 0 }?.let { add(stringResource(R.string.details_total, it)) }
                    }.joinToString(" · ")
                    if (line.isNotBlank()) {
                        Text(
                            text = line,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        )
                    }
                }

                if (details.description.isNotBlank()) {
                    Text(
                        text = details.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f),
                        maxLines = 4,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                val creditLine = buildList {
                    if (details.studios.isNotEmpty()) add(details.studios.take(2).joinToString { it.title })
                    if (details.creators.isNotEmpty()) {
                        add(stringResource(R.string.details_director_prefix, details.creators.take(2).joinToString { it.title }))
                    }
                }.joinToString(" · ")
                if (creditLine.isNotBlank()) {
                    Text(
                        text = creditLine,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                DetailsButtonBar(
                    details = details,
                    isInLibrary = isInLibrary,
                    watchProgress = watchProgress,
                    firstFocusRequester = downFocusRequester,
                    onWatchSelected = onWatchSelected,
                    onLibraryToggle = onLibraryToggle,
                    onEpisodesSelected = onEpisodesSelected,
                    onTrailersSelected = onTrailersSelected,
                    onSimilarSelected = onSimilarSelected,
                    onViewingOrderSelected = onViewingOrderSelected,
                    onScreenshotsSelected = onScreenshotsSelected,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Card(
                modifier = Modifier
                    .width(300.dp)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HeroRatingRow(details: AnimeDetails) {
    val ratings = buildList {
        details.rating.average?.let { add("Yani ${it.formatRating()}") }
        details.rating.kinopoisk?.let { add(stringResource(R.string.details_kinopoisk_rating, it.formatRating())) }
        details.rating.shikimori?.let { add("Shikimori ${it.formatRating()}") }
        details.rating.myAnimeList?.let { add("MAL ${it.formatRating()}") }
    }
    if (ratings.isEmpty()) return
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ratings.forEach { label ->
            Row(
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.height(10.dp).width(10.dp),
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                )
            }
        }
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
