package su.afk.yummy.tv.feature.search.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.search.mobile.R

@Composable
internal fun SheetHeader(
    title: String,
    onBack: (() -> Unit)? = null,
    actionLabel: String? = null,
    actionVisible: Boolean = true,
    onClose: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (onBack != null) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.search_mobile_filters_back),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(horizontal = 80.dp),
        )
        if (actionLabel != null && onClose != null) {
            // The button is always composed so the header keeps a constant height;
            // visibility is animated with alpha only to avoid layout shifts below.
            val actionAlpha by animateFloatAsState(
                targetValue = if (actionVisible) 1f else 0f,
                animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
                label = "sheetHeaderAction",
            )
            TextButton(
                onClick = onClose,
                enabled = actionVisible,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .graphicsLayer { alpha = actionAlpha },
            ) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
internal fun FilterSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        content()
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ChipFlow(content: @Composable () -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        content()
    }
}

@Composable
internal fun YearField(
    label: String,
    value: Int?,
    onValueChanged: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            onValueChanged(text.filter { it.isDigit() }.take(4).toIntOrNull())
        },
        label = { Text(label) },
        singleLine = true,
        shape = RoundedCornerShape(10.dp),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier,
    )
}

@Composable
internal fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
) {
    val shape = CircleShape
    val borderColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
        },
        animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
        label = "filterChipBorder",
    )
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)
        },
        animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
        label = "filterChipBackground",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onBackground
        },
        animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
        label = "filterChipContent",
    )

    Row(
        modifier = modifier
            .clip(shape)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .background(backgroundColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(CHIP_ANIMATION_MILLIS)) +
                    fadeIn(tween(CHIP_ANIMATION_MILLIS)),
            exit = shrinkHorizontally(tween(CHIP_ANIMATION_MILLIS)) +
                    fadeOut(tween(CHIP_ANIMATION_MILLIS)),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier
                    .padding(end = 5.dp)
                    .size(15.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = contentColor,
        )
        trailingIcon?.invoke()
    }
}

/** Chip that toggles sort direction and shows it with a rotating arrow. */
@Composable
internal fun FilterDirectionChip(
    label: String,
    forward: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (forward) 0f else 180f,
        animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
        label = "sortDirectionRotation",
    )
    FilterChip(
        label = label,
        selected = false,
        onClick = onClick,
        modifier = modifier,
        trailingIcon = {
            Icon(
                imageVector = Icons.Filled.ArrowUpward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .padding(start = 5.dp)
                    .size(15.dp)
                    .rotate(rotation),
            )
        },
    )
}

/** Row that opens a nested picker (e.g. genres) with a selection badge and chevron. */
@Composable
internal fun FilterNavigationRow(
    title: String,
    icon: ImageVector,
    selectedCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    val hasSelection = selectedCount > 0
    val iconTint by animateColorAsState(
        targetValue = if (hasSelection) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        animationSpec = tween(durationMillis = CHIP_ANIMATION_MILLIS),
        label = "filterNavIconTint",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        AnimatedVisibility(
            visible = hasSelection,
            enter = expandHorizontally(tween(CHIP_ANIMATION_MILLIS)) +
                    fadeIn(tween(CHIP_ANIMATION_MILLIS)),
            exit = shrinkHorizontally(tween(CHIP_ANIMATION_MILLIS)) +
                    fadeOut(tween(CHIP_ANIMATION_MILLIS)),
        ) {
            Text(
                text = selectedCount.toString(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(end = 8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private const val CHIP_ANIMATION_MILLIS = 180
