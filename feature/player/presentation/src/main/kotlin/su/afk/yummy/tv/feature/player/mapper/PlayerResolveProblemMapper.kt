package su.afk.yummy.tv.feature.player.mapper

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.feature.player.presentation.R

private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404
private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR = 500

/**
 * Человеческое описание проблемы получения потока: что именно не так, а не просто «ошибка».
 * Сначала смотрим на последний сетевой запрос (код ответа или обрыв), затем на шаг, где всё остановилось.
 * Возвращает null, если сказать точнее общей фразы нечего.
 */
fun resolveProblemText(
    strings: StringProvider,
    step: PlayerStreamResolveStatus?,
    details: List<ResolveDetail>,
): String? {
    val last = details.lastOrNull()
    val code = last?.httpCode
    val error = last?.error
    return when {
        last != null && code != null && code >= HTTP_FORBIDDEN -> strings.get(httpProblemRes(code), last.host, code)
        last != null && error != null -> strings.get(networkProblemRes(error), last.host)
        else -> stepProblemRes(step)?.let { strings.get(it) }
    }
}

private fun httpProblemRes(code: Int): Int = when {
    code == HTTP_FORBIDDEN -> R.string.player_problem_http_forbidden
    code == HTTP_NOT_FOUND -> R.string.player_problem_http_not_found
    code == HTTP_TOO_MANY_REQUESTS -> R.string.player_problem_http_too_many
    code >= HTTP_SERVER_ERROR -> R.string.player_problem_http_server
    else -> R.string.player_problem_http_other
}

private fun networkProblemRes(error: String): Int = when {
    error.contains("Timeout", ignoreCase = true) -> R.string.player_problem_network_timeout
    error.contains("UnknownHost", ignoreCase = true) -> R.string.player_problem_network_dns
    error.contains("SSL", ignoreCase = true) || error.contains("Certificate", ignoreCase = true) ->
        R.string.player_problem_network_tls
    error.contains("Connect", ignoreCase = true) -> R.string.player_problem_network_connect
    else -> R.string.player_problem_network_other
}

private fun stepProblemRes(step: PlayerStreamResolveStatus?): Int? = when (step) {
    PlayerStreamResolveStatus.ParsingPage,
    PlayerStreamResolveStatus.LoadingPlayerScript,
    PlayerStreamResolveStatus.DecodingLinks,
    -> R.string.player_problem_parse
    PlayerStreamResolveStatus.SelectingDubbing -> R.string.player_problem_dubbing_missing
    PlayerStreamResolveStatus.EpisodeNotFound -> R.string.player_problem_episode_not_found
    PlayerStreamResolveStatus.PlayerNotFound -> R.string.player_problem_player_not_found
    PlayerStreamResolveStatus.LinkNotFound,
    PlayerStreamResolveStatus.ExtractingLink,
    -> R.string.player_problem_link_not_found
    PlayerStreamResolveStatus.AskingConsent,
    PlayerStreamResolveStatus.RequestingTicket,
    -> R.string.player_problem_ticket
    PlayerStreamResolveStatus.RequestingStream,
    PlayerStreamResolveStatus.CheckingQualities,
    -> R.string.player_problem_stream_not_found
    PlayerStreamResolveStatus.OpeningSession -> R.string.player_problem_session
    PlayerStreamResolveStatus.WaitingForAd -> R.string.player_problem_ad
    else -> null
}
