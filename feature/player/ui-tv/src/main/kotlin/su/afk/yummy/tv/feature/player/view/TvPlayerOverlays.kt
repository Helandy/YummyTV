package su.afk.yummy.tv.feature.player.view

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
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

@Composable
internal fun TvKodikBlockedOverlay(
    message: String,
    modifier: Modifier = Modifier,
) {
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
    }
}

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

@Composable
internal fun TvOverlayButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)

    val bgColor = when {
        focused && primary -> Color.White
        focused -> Color.White.copy(alpha = 0.15f)
        primary -> Color.White.copy(alpha = 0.18f)
        else -> Color.Transparent
    }
    val textColor = if (focused && primary) Color.Black else Color.White
    val borderColor = if (focused) Color.White else Color.White.copy(alpha = 0.35f)

    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = textColor,
        modifier = Modifier
            .border(width = 2.dp, color = borderColor, shape = shape)
            .background(bgColor, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    )
}
