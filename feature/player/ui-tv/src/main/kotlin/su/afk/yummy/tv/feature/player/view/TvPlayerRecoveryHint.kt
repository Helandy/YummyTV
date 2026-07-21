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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.presentation.R

/** Затянувшееся фоновое восстановление Alloha: предлагаем сменить плеер или озвучку. */
@Composable
internal fun TvPlayerRecoveryHint(
    onChangePlayer: (() -> Unit)?,
    onChangeDubbing: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val firstButtonFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstButtonFocusRequester.requestFocus() }
    }
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.80f))
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = stringResource(R.string.player_recovery_hint_title),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (onChangePlayer != null) {
                TvOverlayButton(
                    text = stringResource(R.string.player_change_player),
                    onClick = onChangePlayer,
                    primary = false,
                    modifier = Modifier.focusRequester(firstButtonFocusRequester),
                )
            }
            if (onChangeDubbing != null) {
                TvOverlayButton(
                    text = stringResource(R.string.player_change_dubbing),
                    onClick = onChangeDubbing,
                    primary = false,
                    modifier = if (onChangePlayer == null) {
                        Modifier.focusRequester(firstButtonFocusRequester)
                    } else {
                        Modifier
                    },
                )
            }
        }
    }
}
