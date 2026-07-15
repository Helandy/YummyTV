package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.feature.player.presentation.R
import java.util.Locale

@Composable
fun Int.formatCompactCount(): String = when {
    this >= 1_000_000 -> stringResource(
        R.string.player_count_millions,
        (this / 1_000_000f).formatCompactDecimal(),
    )

    this >= 1_000 -> stringResource(
        R.string.player_count_thousands,
        (this / 1_000f).formatCompactDecimal(),
    )

    else -> toString()
}

private fun Float.formatCompactDecimal(): String =
    if (this % 1f == 0f) {
        toInt().toString()
    } else {
        String.format(Locale.US, "%.1f", this)
    }
