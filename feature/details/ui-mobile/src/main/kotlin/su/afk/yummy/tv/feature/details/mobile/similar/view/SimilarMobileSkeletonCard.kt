package su.afk.yummy.tv.feature.details.mobile.similar.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
internal fun SimilarMobileSkeletonCard(alpha: Float, color: Color) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2f / 3f),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(0.dp),
        )
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp),
                alpha = alpha,
                color = color,
                shape = RoundedCornerShape(4.dp),
            )
            SkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(12.dp),
                alpha = alpha,
                color = color,
                shape = RoundedCornerShape(4.dp),
            )
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
