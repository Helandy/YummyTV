package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.common.PlayerSleepTimerOptions
import su.afk.yummy.tv.feature.player.presentation.R

/** Страница «Автовыключение»: «Отключено» и список интервалов; активный — отмечен галочкой. */
@Composable
internal fun MobilePlayerSleepTimerPage(
    selectedMinutes: Int,
    onSelected: (Int) -> Unit,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        MobilePlayerMenuPageHeader(title = stringResource(R.string.player_sleep_title), onBack = onBack)
        MobilePlayerMenuRow(
            label = stringResource(R.string.player_sleep_off),
            trailing = MobilePlayerMenuRowTrailing.Choice(selectedMinutes == 0),
            onClick = {
                onSelected(0)
                onBack()
            },
        )
        PlayerSleepTimerOptions.minutes.forEach { minutes ->
            MobilePlayerMenuRow(
                label = sleepTimerLabel(minutes),
                trailing = MobilePlayerMenuRowTrailing.Choice(selectedMinutes == minutes),
                onClick = {
                    onSelected(minutes)
                    onBack()
                },
            )
        }
    }
}

@Composable
internal fun sleepTimerLabel(minutes: Int): String =
    if (minutes == 60) {
        stringResource(R.string.player_sleep_hour)
    } else {
        stringResource(R.string.player_sleep_minutes, minutes)
    }
