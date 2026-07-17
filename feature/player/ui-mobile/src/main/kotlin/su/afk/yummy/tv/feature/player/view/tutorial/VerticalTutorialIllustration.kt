package su.afk.yummy.tv.feature.player.view.tutorial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.R

@Composable
internal fun VerticalTutorialIllustration(accent: Color) {
    val progress = rememberTutorialLoopProgress("vertical")
    Box(
        Modifier
            .fillMaxWidth(0.9f)
            .aspectRatio(16f / 9f),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawTutorialScreenFrame()
            drawVerticalRail(size.width * 0.19f, progress, accent)
            drawVerticalRail(size.width * 0.81f, progress, accent)
            drawVerticalArrow(size.width * 0.31f, Color.White.copy(alpha = 0.72f))
            drawVerticalArrow(size.width * 0.69f, Color.White.copy(alpha = 0.72f))
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Filled.BrightnessMedium, contentDescription = null, tint = Color.White)
            Text(
                text = stringResource(R.string.player_mobile_gesture_tutorial_brightness),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null, tint = Color.White)
            Text(
                text = stringResource(R.string.player_mobile_gesture_tutorial_volume),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}
