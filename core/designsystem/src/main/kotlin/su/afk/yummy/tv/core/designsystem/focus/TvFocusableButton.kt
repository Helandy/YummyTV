package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.runtime.Composable

/**
 * Кнопка для пульта: в покое контурная, в фокусе заливается акцентным цветом, поэтому
 * сфокусированную кнопку видно и рядом с соседней (как в диалоге с двумя кнопками).
 */
@Composable
fun TvFocusableButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    TvRetryButton(
        text = text,
        enabled = enabled,
        onClick = onClick,
    )
}
