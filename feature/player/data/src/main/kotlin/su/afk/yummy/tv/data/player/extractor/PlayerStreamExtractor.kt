package su.afk.yummy.tv.data.player.extractor

import android.content.Context
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

internal interface PlayerStreamExtractor {
    fun supports(url: String): Boolean

    suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult
}
