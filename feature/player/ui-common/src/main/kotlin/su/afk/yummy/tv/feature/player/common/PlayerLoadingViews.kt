package su.afk.yummy.tv.feature.player.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.components.YummyLoadingIndicator

@Composable
fun PlayerBlackBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
    )
}

@Composable
fun PlayerBufferingIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        YummyLoadingIndicator(color = Color.White, modifier = modifier, strokeWidth = 2.dp)
    }
}
