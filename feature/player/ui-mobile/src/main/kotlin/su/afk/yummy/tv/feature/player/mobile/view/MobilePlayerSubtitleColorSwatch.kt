package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Кружок-образец цвета субтитров. Выбранный окружён акцентным кольцом с зазором, поэтому соседние
 * цвета не сливаются, как это было у чипов с цветным текстом.
 */
@Composable
internal fun MobilePlayerSubtitleColorSwatch(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ringColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .border(BorderStroke(2.dp, ringColor), CircleShape)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(6.dp)
            .semantics {
                contentDescription = label
                this.selected = selected
                role = Role.RadioButton
            }
            .clip(CircleShape)
            .background(color)
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                CircleShape,
            ),
    )
}
