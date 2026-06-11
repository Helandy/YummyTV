package su.afk.yummy.tv.feature.player.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
internal fun MobilePlayerZoomIndicator(
    visible: Boolean,
    scale: Float,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .background(Color.Black.copy(alpha = 0.78f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = scale.zoomIndicatorLabel(),
                color = Color.White,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Float.zoomIndicatorLabel(): String {
    val tenths = (coerceAtLeast(1f) * 10f).roundToInt()
    val whole = tenths / 10
    val fraction = tenths % 10
    return if (fraction == 0) {
        "x$whole"
    } else {
        "x$whole.$fraction"
    }
}
