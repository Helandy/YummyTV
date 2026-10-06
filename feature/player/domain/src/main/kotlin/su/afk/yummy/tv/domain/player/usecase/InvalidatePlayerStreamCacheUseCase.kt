package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.repository.PlayerStreamRepository
import javax.inject.Inject

/**
 * Забывает закэшированный поток источника, чтобы следующий запрос резолвил его заново.
 *
 * Нужен после переезда на резервный узел CDN: в кэше остались ссылки на отказавший узел, и без
 * сброса повторный вход в плеер в пределах TTL поднял бы их снова.
 */
class InvalidatePlayerStreamCacheUseCase @Inject constructor(
    private val repository: PlayerStreamRepository,
) {
    operator fun invoke(iframeUrl: String) = repository.invalidateResolveCache(iframeUrl)
}
