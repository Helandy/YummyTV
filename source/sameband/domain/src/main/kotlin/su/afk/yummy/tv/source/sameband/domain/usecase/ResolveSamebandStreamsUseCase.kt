package su.afk.yummy.tv.source.sameband.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.sameband.domain.model.SamebandStreamsResult
import su.afk.yummy.tv.source.sameband.domain.model.SamebandTarget
import su.afk.yummy.tv.source.sameband.domain.repository.SamebandRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в HLS-потоки Sameband (480/720/1080p): страница тайтла, плеер и плейлист читаются в момент
 * воспроизведения. Отсутствие тайтла, плеера или серии — [SamebandStreamsResult.NotFound], сбой сети — [SamebandStreamsResult.Failed].
 */
class ResolveSamebandStreamsUseCase @Inject constructor(
    private val repository: SamebandRepository,
) {
    suspend operator fun invoke(target: SamebandTarget): SamebandStreamsResult =
        try {
            repository.getStreams(target)?.let { SamebandStreamsResult.Success(it) } ?: SamebandStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            SamebandStreamsResult.Failed(error.message)
        }
}
