package su.afk.yummy.tv.feature.player.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.utils.formatting.toCompactDecimal
import su.afk.yummy.tv.feature.player.presentation.R

@Composable
fun Int.formatCompactCount(): String = when {
    this >= 1_000_000 -> stringResource(
        R.string.player_count_millions,
        (this / 1_000_000f).toCompactDecimal(),
    )

    this >= 1_000 -> stringResource(
        R.string.player_count_thousands,
        (this / 1_000f).toCompactDecimal(),
    )

    else -> toString()
}
