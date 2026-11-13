package su.afk.yummy.tv.feature.details.similar.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvTitleCardDimensions
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.TvCardSpacing


@Composable
internal fun SimilarLoadingState(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "similar_loading")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "similar_loading_alpha",
    )
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val brightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clipToBounds(),
            horizontalArrangement = Arrangement.spacedBy(
                space = TvCardSpacing.Horizontal,
                alignment = Alignment.CenterHorizontally,
            ),
        ) {
            repeat(6) { index ->
                SimilarSkeletonCard(
                    alpha = alpha,
                    color = if (index == 0) brightColor else baseColor,
                )
            }
        }
    }
}

@Composable
private fun SimilarSkeletonCard(alpha: Float, color: Color) {
    // Ширина и пропорции постера — как у TvTitleCard, под постером название в две строки (bodyLarge, 24dp).
    Column(modifier = Modifier.width(currentTvTitleCardDimensions().width)) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(12.dp),
        )
        Column(modifier = Modifier.padding(10.dp)) {
            repeat(2) { index ->
                Box(Modifier.fillMaxWidth().height(24.dp), contentAlignment = Alignment.CenterStart) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 0) 0.9f else 0.6f)
                            .height(16.dp),
                        alpha = alpha,
                        color = color,
                        shape = RoundedCornerShape(5.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    alpha: Float,
    color: Color,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(color, shape),
    )
}
