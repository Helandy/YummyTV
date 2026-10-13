package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Промпт конца серии: вторичное действие слева, основное справа. Фокус по умолчанию получает
 * основное через [primaryFocusRequester] (его запрашивает TvPlayerFocusEffects).
 */
@Composable
internal fun TvPlayerEndPrompt(
    visible: Boolean,
    title: String,
    primaryLabel: String,
    secondaryLabel: String,
    primaryFocusRequester: FocusRequester,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 28.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TvControlButton(
                    onClick = onSecondary,
                    onFocused = onInteraction,
                ) { color ->
                    Text(
                        text = secondaryLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                    )
                }
                TvControlButton(
                    onClick = onPrimary,
                    onFocused = onInteraction,
                    focusRequester = primaryFocusRequester,
                    primary = true,
                ) { color ->
                    Text(
                        text = primaryLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = color,
                    )
                }
            }
        }
    }
}
