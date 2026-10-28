package su.afk.yummy.tv.source.animeon.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.animeon.domain.model.AnimeOnTarget
import su.afk.yummy.tv.source.animeon.domain.repository.AnimeOnRepository
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import javax.inject.Inject

/**
 * Превращает адрес серии в HLS-потоки AnimeON (плеер Ashdi): id серии и ссылка на плеер читаются в момент воспроизведения.
 * Отсутствие серии или файла — [AshdiStreamsResult.NotFound], закрытый для страны плеер — [AshdiStreamsResult.RegionBlocked], сбой
 * сети — [AshdiStreamsResult.Failed].
 */
class ResolveAnimeOnStreamsUseCase @Inject constructor(
    private val repository: AnimeOnRepository,
) {
    suspend operator fun invoke(target: AnimeOnTarget): AshdiStreamsResult =
        try {
            repository.getStreams(target)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AshdiStreamsResult.Failed(error.message)
        }
}
