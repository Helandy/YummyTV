package su.afk.yummy.tv.feature.library.view

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.feature.library.R

@Composable
internal fun LibraryDeleteButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LibraryActionButton(
        label = stringResource(R.string.library_delete),
        icon = Icons.Filled.Delete,
        focusedContainerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.92f),
        focusedContentColor = MaterialTheme.colorScheme.onError,
        focusedBorderColor = MaterialTheme.colorScheme.onError,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
internal fun LibraryDetailsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    LibraryActionButton(
        label = stringResource(R.string.library_details),
        icon = Icons.Filled.Info,
        focusedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.92f),
        focusedContentColor = MaterialTheme.colorScheme.onPrimary,
        focusedBorderColor = MaterialTheme.colorScheme.onPrimary,
        onClick = onClick,
        modifier = modifier,
    )
}

@Composable
private fun LibraryActionButton(
    label: String,
    icon: ImageVector,
    focusedContainerColor: Color,
    focusedContentColor: Color,
    focusedBorderColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val containerColor = if (focused) {
        focusedContainerColor
    } else {
        Color.Black.copy(alpha = 0.68f)
    }
    val contentColor = if (focused) {
        focusedContentColor
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.92f)
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp)
            .background(containerColor, shape)
            .tvFocusableClick(
                onClick = onClick,
                shape = shape,
                interactionSource = interactionSource,
                focusedScale = 1.04f,
                focusedBorderColor = focusedBorderColor,
            )
            .padding(horizontal = 9.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
