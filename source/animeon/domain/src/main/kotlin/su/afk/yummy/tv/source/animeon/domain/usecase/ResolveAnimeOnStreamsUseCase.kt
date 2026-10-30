package su.afk.yummy.tv.source.animeon.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnStreamsResult
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.animeon.domain.repository.AnimeOnRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в потоки AnimeON (плееры Ashdi, Fenix и Moon): id серии и ссылка на плеер читаются в момент воспроизведения.
 * Отсутствие серии или файла — [AnimeOnStreamsResult.NotFound], закрытый для страны плеер — [AnimeOnStreamsResult.RegionBlocked], сбой
 * сети — [AnimeOnStreamsResult.Failed].
 */
class ResolveAnimeOnStreamsUseCase @Inject constructor(
    private val repository: AnimeOnRepository,
) {
    suspend operator fun invoke(target: AnimeOnTarget): AnimeOnStreamsResult =
        try {
            repository.getStreams(target)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AnimeOnStreamsResult.Failed(error.message)
        }
}
