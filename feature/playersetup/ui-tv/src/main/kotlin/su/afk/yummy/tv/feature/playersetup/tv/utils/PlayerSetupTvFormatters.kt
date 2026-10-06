package su.afk.yummy.tv.feature.playersetup.tv.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.core.model.settings.PreferredVideoQuality
import su.afk.yummy.tv.core.model.settings.YaniContentLanguage
import su.afk.yummy.tv.feature.playersetup.tv.R

@Composable
internal fun PreferredVideoQuality.setupLabel(): String =
    height?.let { "${it}p" } ?: stringResource(R.string.player_setup_tv_quality_best)

internal fun PreferredVideoQuality.next(): PreferredVideoQuality =
    PreferredVideoQuality.entries[(ordinal + 1) % PreferredVideoQuality.entries.size]

@Composable
internal fun YaniContentLanguage.setupLabel(): String = stringResource(
    when (this) {
        YaniContentLanguage.RUSSIAN -> R.string.player_setup_tv_language_russian
        YaniContentLanguage.ENGLISH -> R.string.player_setup_tv_language_english
        YaniContentLanguage.UKRAINIAN -> R.string.player_setup_tv_language_ukrainian
    },
)

internal fun YaniContentLanguage.next(): YaniContentLanguage =
    YaniContentLanguage.entries[(ordinal + 1) % YaniContentLanguage.entries.size]
