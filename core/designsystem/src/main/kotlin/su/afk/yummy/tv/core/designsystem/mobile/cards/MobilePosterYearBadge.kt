package su.afk.yummy.tv.core.designsystem.mobile.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Плашка «год · сезон» в углу постера (настройка «Отображать год у тайтлов в библиотеке»). */
@Composable
fun MobilePosterYearBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        // Постер в списке узкий: «2026 · Осень» должно влезать в одну строку.
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.inverseSurface,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .background(MaterialTheme.colorScheme.inverseOnSurface, RoundedCornerShape(4.dp))
            .padding(horizontal = 3.dp, vertical = 1.dp),
    )
}
