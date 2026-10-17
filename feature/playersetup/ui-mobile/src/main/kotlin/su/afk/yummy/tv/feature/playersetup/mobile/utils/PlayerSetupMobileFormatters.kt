package su.afk.yummy.tv.feature.playersetup.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.feature.playersetup.presentation.R

@Composable
internal fun PreferredVideoQuality.setupLabel(): String =
    height?.let { "${it}p" } ?: stringResource(R.string.player_setup_quality_best)

@Composable
internal fun PreferredVideoQuality.setupHint(): String = stringResource(
    if (this == PreferredVideoQuality.BEST) {
        R.string.player_setup_mobile_quality_best_hint
    } else {
        R.string.player_setup_quality_hint
    },
)
