package su.afk.yummy.tv.data.player.extractor.extra

import android.content.Context
import su.afk.yummy.tv.core.common.player.isExtraSourceUrl
import su.afk.yummy.tv.data.player.extractor.PlayerStreamExtractor
import su.afk.yummy.tv.domain.player.model.PlayerStreamRequest
import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult
import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import javax.inject.Inject

/** Передаёт `source://` подключённым модулям-источникам. Без модулей набор пуст, ссылка считается неподдерживаемой. */
internal class ExtraSourceExtractor @Inject constructor(
    private val repositories: Set<@JvmSuppressWildcards ExtraStreamRepository>,
) : PlayerStreamExtractor {

    override fun supports(url: String): Boolean =
        url.isExtraSourceUrl() && repositories.any { it.supports(url) }

    override suspend fun extract(
        request: PlayerStreamRequest,
        context: Context,
    ): PlayerStreamResolveResult {
        val repository = repositories.firstOrNull { it.supports(request.iframeUrl) }
            ?: return PlayerStreamResolveResult.Unsupported
        return repository.resolve(request.iframeUrl)
    }
}
