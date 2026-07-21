package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.error.StringProvider
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.feature.player.presentation.R

internal fun String.qualityHeight(): Int? =
    Regex("""\d+""").find(this)?.value?.toIntOrNull()

internal fun PlayerStreamResolveResult.KodikBlocked.toMessage(strings: StringProvider): String =
    message
        ?: statusCode?.let { strings.get(R.string.player_server_error, it) }
        ?: strings.get(R.string.player_kodik_blocked)
