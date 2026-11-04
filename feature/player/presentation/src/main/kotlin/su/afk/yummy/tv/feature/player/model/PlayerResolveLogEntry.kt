package su.afk.yummy.tv.feature.player.model

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail

/**
 * Строка журнала получения потока: либо шаг ([status]), либо подробности сетевого запроса ([detail]).
 * [elapsedMs] — время с начала резолва.
 */
data class PlayerResolveLogEntry(
    val status: PlayerStreamResolveStatus? = null,
    val detail: ResolveDetail? = null,
    val elapsedMs: Long,
)
