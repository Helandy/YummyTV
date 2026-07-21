package su.afk.yummy.tv.feature.reviews.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick

/** Кнопка действия с видимым фокусом (масштаб + рамка через [tvFocusableClick]). */
@Composable
internal fun ReviewActionButton(
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .tvFocusableClick(onClick = onClick, shape = shape)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), shape)
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            label,
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}
