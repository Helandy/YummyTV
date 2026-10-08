package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.model.PlayerStreamUnavailableCause
import su.afk.yummy.tv.feature.player.presentation.R
import java.net.URI

/** Хост потока без пути и query: подписанные параметры ссылки в аналитику не попадают. */
internal fun String.streamHost(): String? =
    runCatching { URI(this).host }.getOrNull()?.takeIf { it.isNotBlank() }

internal fun String.qualityHeight(): Int? =
    Regex("""\d+""").find(this)?.value?.toIntOrNull()

internal fun PlayerStreamResolveResult.KodikBlocked.toMessage(strings: StringProvider): String =
    message
        ?: statusCode?.let { strings.get(R.string.player_server_error, it) }
        ?: strings.get(R.string.player_kodik_blocked)

/** Текст для пользователя: название причины, если источник её назвал, иначе общая «озвучка недоступна». */
internal fun PlayerStreamResolveResult.Unavailable.toMessage(strings: StringProvider): String =
    message ?: when (cause) {
        PlayerStreamUnavailableCause.VideoNotFound -> strings.get(R.string.player_video_not_found)
        PlayerStreamUnavailableCause.AccessForbidden -> strings.get(R.string.player_access_forbidden)
        PlayerStreamUnavailableCause.RegionBlocked -> strings.get(R.string.player_region_blocked)
        null -> strings.get(R.string.player_dubbing_unavailable)
    }
