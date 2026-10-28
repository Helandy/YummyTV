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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.feature.player.presentation.R
import su.afk.yummy.tv.core.designsystem.R as CoreR

@Composable
internal fun TvStreamErrorOverlay(
    message: String,
    heading: String? = null,
    onRetry: (() -> Unit)?,
    onChangePlayer: (() -> Unit)? = null,
    onChangeDubbing: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val retryFocusRequester = remember { FocusRequester() }
    if (onRetry != null) {
        LaunchedEffect(Unit) { requestFocusUntilTimeout(retryFocusRequester) }
    }

    Column(
        modifier = modifier
            .padding(12.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (heading != null) {
            Text(
                text = stringResource(R.string.player_balancer_error_title, heading),
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.60f),
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null || onChangePlayer != null || onChangeDubbing != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onRetry != null) {
                    TvOverlayButton(
                        text = stringResource(CoreR.string.common_retry),
                        onClick = onRetry,
                        modifier = Modifier.focusRequester(retryFocusRequester),
                    )
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
