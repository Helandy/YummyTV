package su.afk.yummy.tv.core.designsystem.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors

/**
 * Кнопка ТВ-диалога вместо [androidx.compose.material3.TextButton]: у голого текста без рамки на
 * пульте не видно, что выбрано. В покое — контур, при фокусе — заливка и рамка потолще.
 * [destructive] красит разрушающее действие (удалить, выйти без сохранения) в семантический красный.
 */
@Composable
fun TvDialogButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colorScheme = MaterialTheme.colorScheme
    val accent = if (destructive) YummySemanticColors.Destructive else colorScheme.primary
    val onAccent = if (destructive) Color.White else colorScheme.onPrimary
    val restingContent = if (destructive) YummySemanticColors.Destructive else colorScheme.onSurface
    val shape = RoundedCornerShape(8.dp)
    val isFocused = focused && enabled

    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isFocused) accent else Color.Transparent,
            contentColor = if (isFocused) onAccent else restingContent,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            width = if (isFocused) 2.dp else 1.dp,
            color = when {
                !enabled -> colorScheme.outline.copy(alpha = 0.28f)
                isFocused -> accent
                destructive -> YummySemanticColors.Destructive.copy(alpha = 0.72f)
                else -> colorScheme.outline.copy(alpha = 0.72f)
            },
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        content = content,
    )
}
