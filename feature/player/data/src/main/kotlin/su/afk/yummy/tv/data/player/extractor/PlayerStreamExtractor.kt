package su.afk.yummy.tv.data.player.extractor

import android.content.Context
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

internal interface PlayerStreamExtractor {
    fun supports(url: String): Boolean

    /** Экстрактор сам сообщает свои шаги через `request.onStatus`; сетевой слой для него отдаёт только подробности запросов. */
    val reportsOwnProgress: Boolean get() = false

    suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult
}
