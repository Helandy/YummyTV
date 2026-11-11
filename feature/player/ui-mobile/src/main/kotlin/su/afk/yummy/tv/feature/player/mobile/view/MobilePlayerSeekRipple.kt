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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.seconds
import su.afk.yummy.tv.feature.player.common.model.StepSeekDirection
import su.afk.yummy.tv.feature.player.common.toastIcon

internal val MOBILE_PLAYER_SEEK_TOAST_DURATION = 2.seconds

/**
 * Подсветка половины экрана при двойном тапе (как у YouTube): серая «капля» с иконкой и суммарным
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
                .fillMaxWidth(0.5f)
                .align(if (backward) Alignment.CenterStart else Alignment.CenterEnd),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Color.White.copy(alpha = 0.18f),
                        if (backward) {
                            RoundedCornerShape(topEndPercent = 50, bottomEndPercent = 50)
                        } else {
                            RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                        },
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
