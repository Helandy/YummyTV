package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.model.PlayerCachePurgeResult
import su.afk.yummy.tv.domain.player.repository.PlayerPlaybackCacheRepository
import javax.inject.Inject

/**
 * Выбрасывает дисковый кэш онлайн-воспроизведения.
 *
 * Нужен, когда плеер упёрся в повреждённый контейнер или манифест: кэшированные байты отдаются из
 * кэша по тому же ключу при любом перерезолве и смене узла CDN, поэтому единственный способ
 * перечитать файл целиком — убрать их. Кэш офлайн-загрузок не затрагивается.
 */
class PurgePlayerPlaybackCacheUseCase @Inject constructor(
    private val repository: PlayerPlaybackCacheRepository,
) {
    suspend operator fun invoke(): PlayerCachePurgeResult = repository.purge()
}
