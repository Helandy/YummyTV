package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Thin indeterminate ring for use on top of video.
 *
 * A translucent dark ring is drawn under the white one (a little thicker and shifted down) so the
 * loader stays visible on bright frames. Both rings run the same indeterminate animation, so they
 * stay in sync.
 */
@Composable
fun YummyPlayerLoadingIndicator(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    color: Color = Color.White,
    strokeWidth: Dp = 2.dp,
) {
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .offset(y = 1.dp),
            color = Color.Black.copy(alpha = 0.5f),
            strokeWidth = strokeWidth + 2.dp,
        )
        CircularProgressIndicator(
            modifier = Modifier
                .fillMaxSize()
                .padding(1.dp),
            color = color,
            strokeWidth = strokeWidth,
        )
    }
}
