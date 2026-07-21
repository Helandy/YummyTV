package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.model.anime.AnimeDetails

/**
 * Blurred, cropped poster behind the hero content, dimmed by a scrim so the
 * text stays readable. On devices without RenderEffect support (API < 31)
 * blur is a no-op — the scrim alone keeps contrast.
 */
@Composable
internal fun HeroBackdrop(details: AnimeDetails) {
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
