package su.afk.yummy.tv.feature.details.mobile.similar.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterTextPadding
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterTitleStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.Alignment
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
import su.afk.yummy.tv.core.designsystem.dimensions.TITLE_POSTER_ASPECT_RATIO

@Composable
internal fun SimilarMobileSkeletonCard(alpha: Float, color: Color) {
    val titleLineHeight = with(LocalDensity.current) {
        currentMobilePosterTitleStyle().lineHeight.takeIf { it.isSp }?.toDp() ?: 20.dp
    }
    // Повторяет MobilePosterCard: тот же фон, форма, пропорции постера и название в две строки.
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        SkeletonBlock(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(TITLE_POSTER_ASPECT_RATIO),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(0.dp),
        )
        Column(
            modifier = Modifier.padding(horizontal = currentMobilePosterTextPadding(), vertical = 8.dp),
        ) {
            repeat(2) { index ->
                Box(Modifier.fillMaxWidth().height(titleLineHeight), contentAlignment = Alignment.CenterStart) {
                    SkeletonBlock(
                        modifier = Modifier
                            .fillMaxWidth(if (index == 0) 0.9f else 0.6f)
                            .height(12.dp),
                        alpha = alpha,
                        color = color,
                        shape = RoundedCornerShape(4.dp),
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
