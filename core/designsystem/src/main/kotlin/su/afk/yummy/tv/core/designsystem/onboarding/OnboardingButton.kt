package su.afk.yummy.tv.core.designsystem.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Кнопка онбординга: одинаково работает под пальцем и под D-pad (при фокусе заливается белым). */
@Composable
internal fun OnboardingButton(
    text: String,
    accent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val background = when {
        focused -> Color.White
        accent -> MaterialTheme.colorScheme.primary
        else -> Color.White.copy(alpha = 0.10f)
    }
    val contentColor = when {
        focused -> Color.Black
        accent -> MaterialTheme.colorScheme.onPrimary
        else -> Color.White
    }
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = contentColor,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
    )
}
