package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.toastIcon
import kotlin.time.Duration.Companion.milliseconds

internal val MOBILE_PLAYER_SEEK_TOAST_DURATION = 800.milliseconds

/**
 * Подсветка ~30% ширины экрана при двойном тапе (как у YouTube): серая «капля» у края с иконкой и суммарным
 * смещением («+10 сек»). Ничего не перехватывает - жесты работают под ней.
 */
@Composable
internal fun MobilePlayerSeekRipple(
    text: String?,
    direction: StepSeekDirection,
    modifier: Modifier = Modifier,
) {
    val backward = direction == StepSeekDirection.Backward
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = text != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.3f)
                .align(if (backward) Alignment.CenterStart else Alignment.CenterEnd),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = 0.18f),
                        SeekDropShape(backward),
                    ),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = direction.toastIcon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp),
                    )
                    Text(
                        text = text.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                    )
                }
            }
        }
    }
}

/** Капля: ровный край у границы экрана, выпуклая дуга в сторону центра. */
private class SeekDropShape(private val backward: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (backward) {
                moveTo(0f, 0f)
                cubicTo(w * 1.25f, h * 0.12f, w * 1.25f, h * 0.88f, 0f, h)
            } else {
                moveTo(w, 0f)
                cubicTo(-w * 0.25f, h * 0.12f, -w * 0.25f, h * 0.88f, w, h)
            }
            close()
        }
        return Outline.Generic(path)
    }
}
