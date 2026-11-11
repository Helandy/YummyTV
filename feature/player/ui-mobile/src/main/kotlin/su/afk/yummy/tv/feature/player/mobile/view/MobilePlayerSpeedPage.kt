package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.player.mobile.utils.MOBILE_PLAYER_SPEED_MAX
import su.afk.yummy.tv.feature.player.mobile.utils.MOBILE_PLAYER_SPEED_MIN
import su.afk.yummy.tv.feature.player.mobile.utils.MOBILE_PLAYER_SPEED_SLIDER_STEPS
import su.afk.yummy.tv.feature.player.mobile.utils.MobilePlayerSpeedChips
import su.afk.yummy.tv.feature.player.mobile.utils.toMobilePlayerSpeed
import su.afk.yummy.tv.feature.player.mobile.utils.toSpeedChipLabel
import su.afk.yummy.tv.feature.player.mobile.utils.toSpeedTitle
import su.afk.yummy.tv.feature.player.presentation.R

private const val SPEED_CHIPS_PER_ROW = 5
private const val SPEED_BUTTON_STEP = 0.05f

/**
 * Страница «Скорость воспроизведения»: заголовок со значением, ползунок с кнопками −/+ и сетка
 * быстрых значений. Чипы и кнопки применяются сразу, ползунок — когда палец отпущен.
 */
@Composable
internal fun MobilePlayerSpeedPage(
    speed: Float,
    onSpeedSelected: (Float) -> Unit,
    onBack: () -> Unit,
) {
    // Пока тянем ползунок, показываем его значение, а не сохранённое.
    var dragging by remember { mutableStateOf<Float?>(null) }
    val shown = dragging ?: speed
    Column(modifier = Modifier.fillMaxWidth()) {
        MobilePlayerMenuPageHeader(
            title = stringResource(R.string.player_speed_title),
            onBack = onBack,
        )
        Text(
            text = shown.toSpeedTitle(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilledTonalIconButton(onClick = { onSpeedSelected((speed - SPEED_BUTTON_STEP).toMobilePlayerSpeed()) }) {
                Icon(Icons.Filled.Remove, contentDescription = stringResource(R.string.player_speed_slower))
            }
            Slider(
                value = shown,
                onValueChange = { dragging = it.toMobilePlayerSpeed() },
                onValueChangeFinished = {
                    dragging?.let(onSpeedSelected)
                    dragging = null
                },
                valueRange = MOBILE_PLAYER_SPEED_MIN..MOBILE_PLAYER_SPEED_MAX,
                steps = MOBILE_PLAYER_SPEED_SLIDER_STEPS,
                modifier = Modifier.weight(1f),
            )
            FilledTonalIconButton(onClick = { onSpeedSelected((speed + SPEED_BUTTON_STEP).toMobilePlayerSpeed()) }) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.player_speed_faster))
            }
        }
        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MobilePlayerSpeedChips.chunked(SPEED_CHIPS_PER_ROW).forEach { rowChips ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rowChips.forEach { chip ->
                        MobilePlayerSpeedChip(
                            value = chip,
                            selected = chip == speed.toMobilePlayerSpeed(),
                            onClick = { onSpeedSelected(chip) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MobilePlayerSpeedChip(
    value: Float,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clickable(onClick = onClick),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = value.toSpeedChipLabel(), style = MaterialTheme.typography.bodyLarge)
            }
        }
        if (value == 1f) {
            Text(
                text = stringResource(R.string.player_speed_normal),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
