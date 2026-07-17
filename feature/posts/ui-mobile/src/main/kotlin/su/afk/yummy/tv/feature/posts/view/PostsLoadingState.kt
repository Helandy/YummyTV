package su.afk.yummy.tv.feature.posts.view

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun PostsLoadingState(
    modifier: Modifier = Modifier,
    cardCount: Int = 2,
) {
    val transition = rememberInfiniteTransition(label = "posts_loading")
    val alpha by transition.animateFloat(
        initialValue = 0.38f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "posts_loading_alpha",
    )
    val skeletonColor = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        repeat(cardCount) {
            PostLoadingCard(skeletonColor)
        }
    }
}

@Composable
private fun PostLoadingCard(skeletonColor: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(skeletonColor),
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SkeletonBlock(
                    modifier = Modifier
                        .width(88.dp)
                        .height(28.dp),
                    color = skeletonColor,
                    shape = RoundedCornerShape(14.dp),
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.82f)
                        .height(24.dp),
                    color = skeletonColor,
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(15.dp),
                    color = skeletonColor,
                )
                SkeletonBlock(
                    modifier = Modifier
                        .fillMaxWidth(0.68f)
                        .height(15.dp),
                    color = skeletonColor,
                )
                SkeletonBlock(
                    modifier = Modifier
                        .width(132.dp)
                        .height(13.dp),
                    color = skeletonColor,
                )
            }
        }
    }
}

@Composable
private fun SkeletonBlock(
    modifier: Modifier,
    color: Color,
    shape: RoundedCornerShape = RoundedCornerShape(6.dp),
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(color)
    )
}
