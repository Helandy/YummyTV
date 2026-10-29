package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.dimensions.currentMobilePosterBadgeScale

/** Тип тайтла (Сериал, Фильм…) в левом нижнем углу постера; для слота `posterOverlay` у [MobilePosterCard]. */
@Composable
fun BoxScope.MobilePosterType(type: String) {
    val scale = currentMobilePosterBadgeScale()
    Text(
        text = type,
        // Размер как у RatingBadge: карточки в сетке узкие, крупный год их перебивает.
        style = MaterialTheme.typography.labelSmall.let { it.copy(fontSize = it.fontSize * scale) },
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.inverseSurface,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(4.dp)
            .background(
                MaterialTheme.colorScheme.inverseOnSurface,
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 5.dp * scale, vertical = 2.dp * scale),
    )
}
