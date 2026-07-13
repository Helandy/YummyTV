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
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvCardSpacing

private val SkeletonCardWidth = 188.dp
private val SkeletonPosterHeight = 258.dp

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
    Column(
        modifier = Modifier.width(SkeletonCardWidth),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(SkeletonPosterHeight),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(8.dp),
        )
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(5.dp),
        )
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(14.dp),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(5.dp),
        )
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
