package su.afk.yummy.tv.feature.details.view.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Порядковый номер тайтла в левом верхнем углу постера. */
@Composable
internal fun BoxScope.TvPosterIndex(index: Int) {
    Text(
        text = index.toString().padStart(2, '0'),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(4.dp)
            .background(Color.Black.copy(alpha = 0.58f), RoundedCornerShape(5.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}
