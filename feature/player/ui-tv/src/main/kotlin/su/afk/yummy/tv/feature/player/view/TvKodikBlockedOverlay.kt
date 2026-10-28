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
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
internal fun TvKodikBlockedOverlay(
    message: String,
    onChangePlayer: (() -> Unit)? = null,
    onChangeDubbing: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val firstButtonFocusRequester = remember { FocusRequester() }
    val hasActions = onChangePlayer != null || onChangeDubbing != null
    if (hasActions) {
        LaunchedEffect(Unit) { requestFocusUntilTimeout(firstButtonFocusRequester) }
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.85f))
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.player_kodik_error_title),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.60f),
        )
        Text(
            text = stringResource(R.string.player_quoted_message, message),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        if (hasActions) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (onChangePlayer != null) {
                    TvOverlayButton(
                        text = stringResource(R.string.player_change_player),
                        onClick = onChangePlayer,
                        modifier = Modifier.focusRequester(firstButtonFocusRequester),
                    )
                }
                if (onChangeDubbing != null) {
                    TvOverlayButton(
                        text = stringResource(R.string.player_change_dubbing),
                        onClick = onChangeDubbing,
                        modifier = if (onChangePlayer == null) {
                            Modifier.focusRequester(firstButtonFocusRequester)
                        } else {
                            Modifier
                        },
                        primary = onChangePlayer == null,
                    )
                }
            }
        }
    }
}
