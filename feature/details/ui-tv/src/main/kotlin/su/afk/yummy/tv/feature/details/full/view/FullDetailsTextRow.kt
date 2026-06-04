package su.afk.yummy.tv.feature.details.full.view

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight

@Composable
internal fun FullDetailsTextRow(label: String, value: String) {
    FullDetailsRow(label = label) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
        )
    }
}
