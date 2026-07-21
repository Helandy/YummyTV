package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
internal fun TvStreamErrorOverlay(
    message: String,
    onRetry: (() -> Unit)?,
    onChangePlayer: (() -> Unit)? = null,
    onChangeDubbing: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null || onChangePlayer != null || onChangeDubbing != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onRetry != null) {
                    TvOverlayButton(text = stringResource(R.string.player_retry), onClick = onRetry)
                }
                if (onChangePlayer != null) {
                    TvOverlayButton(
                        text = stringResource(R.string.player_change_player),
                        onClick = onChangePlayer,
                        primary = false,
                    )
                }
                if (onChangeDubbing != null) {
                    TvOverlayButton(
                        text = stringResource(R.string.player_change_dubbing),
                        onClick = onChangeDubbing,
                        primary = false,
                    )
                }
            }
        }
    }
}
