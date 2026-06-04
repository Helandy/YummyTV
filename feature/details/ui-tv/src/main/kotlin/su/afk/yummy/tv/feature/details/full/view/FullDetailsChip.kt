package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun FullDetailsChip(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.86f),
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                shape = RoundedCornerShape(999.dp),
            )
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}
