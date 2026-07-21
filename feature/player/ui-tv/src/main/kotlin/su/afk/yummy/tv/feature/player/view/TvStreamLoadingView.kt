package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
internal fun TvStreamLoadingView(
    onChangePlayer: (() -> Unit)? = null,
    onChangeDubbing: (() -> Unit)? = null,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = stringResource(R.string.player_loading_stream),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
            }
            if (onChangePlayer != null || onChangeDubbing != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
}
