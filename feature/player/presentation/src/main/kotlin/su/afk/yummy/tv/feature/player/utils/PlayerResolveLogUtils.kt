package su.afk.yummy.tv.feature.player.utils

import kotlinx.collections.immutable.toImmutableList
import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.model.PlayerResolveLogEntry
import java.util.Locale

private const val RESOLVE_LOG_MAX_ENTRIES = 30
private const val MS_IN_SECOND = 1000.0

/** Добавляет шаг в журнал и делает его текущим; подряд идущий тот же шаг не дублируется. */
fun PlayerState.State.withResolveStep(status: PlayerStreamResolveStatus, elapsedMs: Long): PlayerState.State {
    if (resolveLog.lastOrNull()?.status == status) return copy(resolveStatus = status)
    return copy(
        resolveStatus = status,
        resolveLog = (resolveLog + PlayerResolveLogEntry(status = status, elapsedMs = elapsedMs))
            .takeLast(RESOLVE_LOG_MAX_ENTRIES)
            .toImmutableList(),
    )
}

/** Добавляет в журнал подробности сетевого запроса. */
fun PlayerState.State.withResolveDetail(detail: ResolveDetail, elapsedMs: Long): PlayerState.State =
    copy(
        resolveLog = (resolveLog + PlayerResolveLogEntry(detail = detail, elapsedMs = elapsedMs))
            .takeLast(RESOLVE_LOG_MAX_ENTRIES)
            .toImmutableList(),
    )

/** `host · HTTP 200 · 340 ms` или `host · UnknownHostException · 1200 ms`. */
fun ResolveDetail.toText(): String = buildString {
    append(host)
    append(" · ")
    append(httpCode?.let { "HTTP $it" } ?: error ?: "—")
    append(" · ")
    append(elapsedMs)
    append(" ms")
}

/** Время с начала резолва: `+0.4 s`. */
fun Long.toElapsedText(): String = String.format(Locale.US, "+%.1f s", this / MS_IN_SECOND)
