package su.afk.yummy.tv.source.sameband.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.sameband.domain.repository.SamebandRepository
import javax.inject.Inject

/**
 * Список номеров серий тайтла на Sameband по плейлисту плеера: сервер число серий не знает, поэтому они берутся с сайта.
 * Любой сбой (сеть, нет плеера) даёт пустой список, чтобы балансер просто не появился в шторке.
 */
class ListSamebandEpisodesUseCase @Inject constructor(
    private val repository: SamebandRepository,
) {
    suspend operator fun invoke(slug: String): List<Int> =
        try {
            repository.getEpisodes(slug)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyList()
        }
}
