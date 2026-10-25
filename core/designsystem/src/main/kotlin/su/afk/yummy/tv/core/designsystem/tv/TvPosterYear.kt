package su.afk.yummy.tv.core.designsystem.tv

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
import su.afk.yummy.tv.core.designsystem.dimensions.currentTvPosterBadgeScale

/** Год выхода в правом нижнем углу постера; для слота `posterOverlay` у [TvTitleCard]. */
@Composable
fun BoxScope.TvPosterYear(year: Int) {
    val scale = currentTvPosterBadgeScale()
    Text(
        text = year.toString(),
        // Размер как у RatingBadge, чтобы год и рейтинг на постере были соразмерны.
        style = MaterialTheme.typography.labelSmall.let { it.copy(fontSize = it.fontSize * scale) },
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.inverseSurface,
        maxLines = 1,
        softWrap = false,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .background(
                MaterialTheme.colorScheme.inverseOnSurface,
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 5.dp * scale, vertical = 2.dp * scale),
    )
}
