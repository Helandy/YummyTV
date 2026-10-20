package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Промпты по центру не тянутся во всю ширину ТВ: отступ от краёв и предельная ширина. */
internal val TV_PLAYER_PROMPT_SIDE_MARGIN = 64.dp
internal val TV_PLAYER_PROMPT_MAX_WIDTH = 560.dp

/**
 * Промпт конца серии: вторичное действие слева (если есть), основное справа. Фокус по умолчанию получает
 * основное через [primaryFocusRequester] (его запрашивает TvPlayerFocusEffects).
 */
@Composable
internal fun TvPlayerEndPrompt(
    visible: Boolean,
    title: String,
    primaryLabel: String,
    secondaryLabel: String?,
    primaryFocusRequester: FocusRequester,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit,
    onInteraction: () -> Unit,
    modifier: Modifier = Modifier,
    /** Вторая строка под заголовком, как в диалоге со списком озвучек. */
    subtitle: String? = null,
) {
    if (!visible) return

    Box(
        modifier = modifier
            .padding(horizontal = TV_PLAYER_PROMPT_SIDE_MARGIN)
            .widthIn(max = TV_PLAYER_PROMPT_MAX_WIDTH)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.78f))
            .padding(horizontal = 28.dp, vertical = 22.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.72f),
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (secondaryLabel != null) {
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
