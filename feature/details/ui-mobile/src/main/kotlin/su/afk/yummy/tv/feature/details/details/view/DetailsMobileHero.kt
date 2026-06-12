package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.utils.bestUrl
import su.afk.yummy.tv.feature.details.details.utils.formatAiredProgress

@Composable
internal fun DetailsMobileHero(
    state: DetailsState.State,
    details: AnimeDetails,
    onBack: () -> Unit,
    onPosterClick: () -> Unit,
    onTitleClick: () -> Unit,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val posterWidth = (configuration.screenWidthDp.dp * 0.42f).coerceIn(142.dp, 190.dp)
    val posterUrl = details.poster.bestUrl()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AsyncImage(
            model = posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .blur(28.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to Color.Black.copy(alpha = 0.30f),
                            0.36f to Color.Black.copy(alpha = 0.18f),
                            0.54f to MaterialTheme.colorScheme.surface.copy(alpha = 0.26f),
                            0.76f to MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                            1f to MaterialTheme.colorScheme.background,
                        ),
                    ),
                ),
        )
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 58.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            AsyncImage(
                model = posterUrl,
                contentDescription = details.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(posterWidth)
                    .aspectRatio(2f / 3f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(onClick = onPosterClick),
            )
            DetailsRatingRow(
                details = details,
                modifier = Modifier.padding(top = 8.dp),
            )
            Text(
                text = details.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTitleClick),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            DetailsMetaChips(details)
            details.genres.take(5).joinToString(" • ") { it.title }.takeIf { it.isNotBlank() }?.let { genres ->
                Text(
                    text = genres,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            details.episodes?.formatAiredProgress()?.let { progress ->
                Text(
                    text = progress,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
                    textAlign = TextAlign.Center,
                )
            }
            DetailsPrimaryActions(
                state = state,
                details = details,
                onWatchSelected = onWatchSelected,
                onLibraryToggle = onLibraryToggle,
                onFavoriteToggle = onFavoriteToggle,
                modifier = Modifier.padding(top = 6.dp, bottom = 12.dp),
            )
        }
    }
}
