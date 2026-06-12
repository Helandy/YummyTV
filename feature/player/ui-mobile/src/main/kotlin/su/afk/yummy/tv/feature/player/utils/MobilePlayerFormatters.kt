package su.afk.yummy.tv.feature.player.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.util.Locale
import su.afk.yummy.tv.feature.player.presentation.R as PlayerR

internal fun formatMobilePlayerTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

@Composable
internal fun Int.formatCompactCount(): String = when {
    this >= 1_000_000 -> stringResource(
        PlayerR.string.player_count_millions,
        (this / 1_000_000f).formatCompactDecimal(),
    )

    this >= 1_000 -> stringResource(
        PlayerR.string.player_count_thousands,
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
