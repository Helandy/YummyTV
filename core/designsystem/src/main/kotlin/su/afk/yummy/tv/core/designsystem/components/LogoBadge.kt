package su.afk.yummy.tv.core.designsystem.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Логотип студии или балансера на светлой подложке (логотипы рассчитаны на белый фон).
 * Без картинки показывает [fallbackIcon] (у плееров и озвучек она своя) на тёмной подложке.
 */
@Composable
fun LogoBadge(
    @DrawableRes logoRes: Int?,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    fallbackIcon: ImageVector = Icons.Filled.PlayCircle,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size / 5))
            .background(if (logoRes != null) Color.White else Color(0xFF3A3A44)),
    ) {
        if (logoRes != null) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(size).padding(size / 10),
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.6f),
            )
        }
    }
}
