package su.afk.yummy.tv.feature.playersetup.mobile.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PlayerOrientationMode
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.feature.playersetup.presentation.R

@Composable
internal fun PreferredVideoQuality.setupLabel(): String =
    height?.let { "${it}p" } ?: stringResource(R.string.player_setup_quality_best)

@Composable
internal fun PlayerOrientationMode.setupLabel(): String = stringResource(
    when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.player_setup_mobile_orientation_system
        PlayerOrientationMode.LEFT -> R.string.player_setup_mobile_orientation_left
        PlayerOrientationMode.RIGHT -> R.string.player_setup_mobile_orientation_right
    },
)

@Composable
internal fun YaniContentLanguage.setupLabel(): String = stringResource(
    when (this) {
        YaniContentLanguage.RUSSIAN -> R.string.player_setup_language_russian
        YaniContentLanguage.ENGLISH -> R.string.player_setup_language_english
        YaniContentLanguage.UKRAINIAN -> R.string.player_setup_language_ukrainian
    },
)

@Composable
internal fun PlayerOrientationMode.setupHint(): String = stringResource(
    when (this) {
        PlayerOrientationMode.SYSTEM -> R.string.player_setup_mobile_orientation_system_hint
        PlayerOrientationMode.LEFT -> R.string.player_setup_mobile_orientation_left_hint
        PlayerOrientationMode.RIGHT -> R.string.player_setup_mobile_orientation_right_hint
    },
)

@Composable
internal fun PreferredVideoQuality.setupHint(): String = stringResource(
    if (this == PreferredVideoQuality.BEST) {
        R.string.player_setup_mobile_quality_best_hint
    } else {
        R.string.player_setup_quality_hint
    },
)
