package su.afk.yummy.tv.feature.player.view.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
internal fun PlayerSkipSnackbar(
    text: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = fadeIn(),
        exit = fadeOut(),
    ) {
        Text(
            text = text.orEmpty(),
            style = MaterialTheme.typography.titleSmall,
            color = Color.White,
            modifier = Modifier
                .background(Color.Black.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
