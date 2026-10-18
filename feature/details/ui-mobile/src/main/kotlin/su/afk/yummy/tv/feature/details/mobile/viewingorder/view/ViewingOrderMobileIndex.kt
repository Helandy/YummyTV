package su.afk.yummy.tv.feature.details.mobile.viewingorder.view

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

/** Порядковый номер тайтла в левом верхнем углу постера. */
@Composable
internal fun BoxScope.ViewingOrderMobileIndex(index: Int) {
    Text(
        text = index.toString().padStart(2, '0'),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(8.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                RoundedCornerShape(4.dp),
            )
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}
