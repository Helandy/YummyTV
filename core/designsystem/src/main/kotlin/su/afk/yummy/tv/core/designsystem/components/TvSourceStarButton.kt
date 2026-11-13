package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.R
import su.afk.yummy.tv.core.designsystem.motion.YummyMotion

private val StarPaleYellow = Color(0xFFD8C47A)
private val StarDeepYellow = Color(0xFFC9A100)

/**
 * Звезда озвучки/балансера для ТВ: отдельная иконка справа от строки, фокус на неё уходит
 * кнопкой «вправо». В фокусе увеличивается и получает светлую подложку.
 */
@Composable
fun TvSourceStarButton(
    starred: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Карточка под звездой светлая (в фокусе/выбрана): иконка и подложка рисуются тёмными. */
    onLightCard: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.18f else 1f,
        animationSpec = tween(YummyMotion.Short),
        label = "tvSourceStarScale",
    )
    val background by animateColorAsState(
        targetValue = when {
            onLightCard && focused -> Color.Black.copy(alpha = 0.16f)
            onLightCard -> Color.Black.copy(alpha = 0.06f)
            focused -> StarPaleYellow.copy(alpha = 0.22f)
            else -> Color.White.copy(alpha = 0.06f)
        },
        animationSpec = tween(YummyMotion.Short),
        label = "tvSourceStarBackground",
    )
    val tint by animateColorAsState(
        targetValue = when {
            starred && onLightCard -> StarDeepYellow
            starred -> StarPaleYellow
            onLightCard -> Color.Black.copy(alpha = if (focused) 0.85f else 0.55f)
            focused -> Color.White.copy(alpha = 0.9f)
            else -> Color.White.copy(alpha = 0.45f)
        },
        animationSpec = tween(YummyMotion.Short),
        label = "tvSourceStarTint",
    )
    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(CircleShape)
            .background(background)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (starred) Icons.Filled.Star else Icons.Outlined.StarBorder,
            contentDescription = stringResource(
                if (starred) R.string.source_star_remove else R.string.source_star_add,
            ),
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
    }
}
