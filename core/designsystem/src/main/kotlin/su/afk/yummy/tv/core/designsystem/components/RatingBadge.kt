package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun RatingBadge(
    rating: Double,
    modifier: Modifier = Modifier,
    decimals: Int = 1,
    /** Без звезды — там, где рядом стоит оценка пользователя со звездой, чтобы их не путать. */
    showStar: Boolean = true,
    /** Множитель размера плашки: крупные карточки постеров получают пропорционально крупную плашку. */
    scale: Float = 1f,
) {
    val color = rating.toRatingColor()
    val safeDecimals = decimals.coerceAtLeast(0)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp * scale),
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.62f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp * scale, vertical = 2.dp * scale),
    ) {
        if (showStar) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(9.dp * scale),
            )
        }
        Text(
            text = String.format(Locale.US, "%.${safeDecimals}f", rating),
            style = MaterialTheme.typography.labelSmall.let { it.copy(fontSize = it.fontSize * scale) },
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}
