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
import androidx.compose.ui.unit.dp

/** Год выхода в правом нижнем углу постера; для слота `posterOverlay` у [TvTitleCard]. */
@Composable
fun BoxScope.TvPosterYear(year: Int) {
    Text(
        text = year.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(4.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
