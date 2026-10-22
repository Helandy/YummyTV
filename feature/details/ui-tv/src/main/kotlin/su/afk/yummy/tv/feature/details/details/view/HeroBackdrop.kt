package su.afk.yummy.tv.feature.details.details.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import su.afk.yummy.tv.core.model.anime.AnimeDetails

/**
 * Blurred, cropped poster behind the hero content, dimmed by a scrim so the
 * text stays readable. On devices without RenderEffect support (API < 31)
 * blur is a no-op — the scrim alone keeps contrast.
 */
@Composable
internal fun HeroBackdrop(details: AnimeDetails) {
    val surface = MaterialTheme.colorScheme.surface
    val backdropUrl = details.poster?.run { big ?: medium ?: small }
    if (backdropUrl != null) HeroBackdropImage(backdropUrl, surface)
    HeroBottomFade()
}

@Composable
private fun HeroBackdropImage(backdropUrl: String, surface: Color) {
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

/** Плавно растворяет низ хиро в фон ленты, чтобы не было резкой границы перед описанием. */
@Composable
private fun HeroBottomFade() {
    val background = MaterialTheme.colorScheme.background
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(HERO_FADE_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        0f to background.copy(alpha = 0f),
                        0.25f to background.copy(alpha = 0.18f),
                        0.5f to background.copy(alpha = 0.5f),
                        0.75f to background.copy(alpha = 0.82f),
                        1f to background,
                    ),
                ),
        )
    }
}

private val HERO_FADE_HEIGHT = 220.dp
