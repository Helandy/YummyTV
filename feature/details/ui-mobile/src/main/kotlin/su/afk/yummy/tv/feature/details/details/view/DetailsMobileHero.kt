package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.domain.anime.model.AnimeDetails
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.details.utils.bestUrl
import su.afk.yummy.tv.feature.details.details.utils.formatAiredProgress

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DetailsMobileHero(
    state: DetailsState.State,
    details: AnimeDetails,
    onPosterClick: () -> Unit,
    onTitleClick: () -> Unit,
    onWatchSelected: () -> Unit,
    onLibraryToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
) {
    val heroHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.68f).coerceAtLeast(500.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(MaterialTheme.colorScheme.surface),
    ) {
        AsyncImage(
            model = details.poster.bestUrl(),
            contentDescription = details.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
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
                            0.58f to MaterialTheme.colorScheme.surface.copy(alpha = 0.28f),
                            0.82f to MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                            1f to MaterialTheme.colorScheme.surface,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .height(heroHeight * 0.45f)
                .clickable(onClick = onPosterClick),
        )
        DetailsRatingRow(
            details = details,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 16.dp),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = details.title,
                modifier = Modifier.clickable(onClick = onTitleClick),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
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
                )
            }
            DetailsPrimaryActions(
                state = state,
                details = details,
                onWatchSelected = onWatchSelected,
                onLibraryToggle = onLibraryToggle,
                onFavoriteToggle = onFavoriteToggle,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}
