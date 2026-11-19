package su.afk.yummy.tv.feature.library.transfer.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.library.presentation.R
import su.afk.yummy.tv.core.designsystem.R as CoreR

@Composable
internal fun ListTransferTvButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    primary: Boolean = false,
    retainFocusWhenDisabled: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val colors = MaterialTheme.colorScheme

    val bgColor = when {
        !enabled -> colors.surfaceContainerLow.copy(alpha = 0.5f)
        primary && focused -> colors.primary
        primary -> colors.primary.copy(alpha = 0.82f)
        focused -> colors.primary.copy(alpha = 0.16f)
        else -> colors.surfaceContainerHigh
    }
    val contentColor = when {
        !enabled -> colors.onSurfaceVariant.copy(alpha = 0.6f)
        primary -> colors.onPrimary
        focused -> colors.onBackground
        else -> colors.onSurface
    }
    val borderColor = when {
        focused && !enabled -> colors.outline
        focused && primary -> colors.onPrimary
        focused -> colors.primary
        else -> Color.Transparent
    }
    // Во время короткой операции (экспорт / разбор файла) кнопка остаётся в дереве и держит фокус,
    // чтобы Android не уводил DPAD-фокус в боковое меню из-за отключения узла.
    val interactionModifier = if (!enabled && retainFocusWhenDisabled) {
        Modifier
            .semantics(mergeDescendants = true) {
                role = Role.Button
                disabled()
            }
            .focusable(interactionSource = interactionSource)
    } else {
        Modifier.clickable(
            enabled = enabled,
            role = Role.Button,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clip(shape)
            .border(width = 2.dp, color = borderColor, shape = shape)
            .background(bgColor, shape)
            .then(interactionModifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            textAlign = if (icon != null) TextAlign.Start else TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
    }
}

@Composable
internal fun ListTransferTvStatusRow(
    text: String,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.padding(horizontal = 4.dp, vertical = 4.dp),
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(18.dp),
            strokeWidth = 2.dp,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ListTransferTvResultCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) colors.primary else Color.Transparent,
                shape = shape,
            )
            .background(colors.primaryContainer.copy(alpha = 0.45f), shape)
            .focusable(interactionSource = interactionSource)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        content()
    }
}

@Composable
internal fun ListTransferTvRadioRow(
    label: String,
    hint: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val colors = MaterialTheme.colorScheme

    val bgColor = when {
        focused && selected -> colors.primary.copy(alpha = 0.18f)
        focused -> colors.onSurface.copy(alpha = 0.08f)
        selected -> colors.primary.copy(alpha = 0.08f)
        else -> Color.Transparent
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) colors.primary else Color.Transparent,
                shape = shape,
            )
            .background(bgColor, shape)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.onBackground else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun ListTransferTvToggleRow(
    label: String,
    hint: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) colors.primary else Color.Transparent,
                shape = shape,
            )
            .background(
                color = if (focused) colors.onSurface.copy(alpha = 0.08f) else Color.Transparent,
                shape = shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onToggle,
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) colors.onBackground else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
internal fun ListTransferTvErrorBanner(
    title: String?,
    message: String,
    onRetry: (() -> Unit)?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(12.dp)
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.errorContainer, shape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onErrorContainer,
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.onErrorContainer,
            )
        }
        if (onRetry != null) {
            TvErrorBannerAction(
                label = stringResource(CoreR.string.common_retry),
                onClick = onRetry,
            )
        }
        TvErrorBannerAction(
            label = stringResource(R.string.library_transfer_dismiss),
            onClick = onDismiss,
        )
    }
}

@Composable
private fun TvErrorBannerAction(
    label: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(8.dp)

    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = colors.onErrorContainer,
        modifier = Modifier
            .clip(shape)
            .border(
                width = 2.dp,
                color = if (focused) colors.onErrorContainer else Color.Transparent,
                shape = shape,
            )
            .background(
                color = if (focused) {
                    colors.onErrorContainer.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                },
                shape = shape,
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}
