package su.afk.yummy.tv.core.designsystem.presenter.focus

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun TvFocusableButton(
    text: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Button(
        enabled = enabled,
        onClick = onClick,
    ) {
        Text(text)
    }
}

@Composable
fun TvRetryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = when {
        !enabled -> Color.Transparent
        focused -> colorScheme.primary
        else -> Color.Transparent
    }
    val contentColor = when {
        !enabled -> colorScheme.onSurface.copy(alpha = 0.38f)
        focused -> colorScheme.onPrimary
        else -> colorScheme.onSurface
    }
    val borderColor = when {
        !enabled -> colorScheme.outline.copy(alpha = 0.28f)
        focused -> colorScheme.primary
        else -> colorScheme.outline.copy(alpha = 0.72f)
    }

    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = onClick,
        shape = shape,
        interactionSource = interactionSource,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colorScheme.onSurface.copy(alpha = 0.38f),
        ),
        border = BorderStroke(
            width = if (focused && enabled) 2.dp else 1.dp,
            color = borderColor,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) {
        Text(text = text)
    }
}
