package su.afk.yummy.tv.feature.player.mobile.cast

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cast
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.presentation.R

/**
 * Замена пустого видео-кадра, пока активна Cast-сессия: локальный ContentFrame ничего не рисует
 * (кадры уходят на приёмник), поэтому вместо чёрного экрана без единой подсказки показываем статус.
 * Не перехватывает тапы - жесты и таймлайн/контролы под ним продолжают работать как обычно.
 */
@Composable
internal fun MobileCastingIndicator(
    deviceName: String?,
    onStopCasting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Cast,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(64.dp),
        )
        Text(
            text = stringResource(R.string.player_mobile_casting_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(top = 24.dp),
        )
        Text(
            text = deviceName?.let { stringResource(R.string.player_mobile_casting_to, it) }
                ?: stringResource(R.string.player_mobile_casting_to_unknown),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 8.dp),
        )
        OutlinedButton(
            onClick = onStopCasting,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            Text(stringResource(R.string.player_mobile_stop_casting))
        }
    }
}
