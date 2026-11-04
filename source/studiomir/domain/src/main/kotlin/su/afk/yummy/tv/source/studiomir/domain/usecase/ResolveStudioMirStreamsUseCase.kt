package su.afk.yummy.tv.source.studiomir.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirStreamsResult
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget
import su.afk.yummy.tv.source.studiomir.domain.repository.StudioMirRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в HLS-потоки Studio MIR: плейлист серий лежит прямо в HTML страницы тайтла и меняется по мере выхода серий, поэтому
 * страница читается в момент воспроизведения. Отсутствие тайтла, плеера или серии — [StudioMirStreamsResult.NotFound], сбой сети —
 * [StudioMirStreamsResult.Failed].
 */
class ResolveStudioMirStreamsUseCase @Inject constructor(
    private val repository: StudioMirRepository,
) {
    suspend operator fun invoke(target: StudioMirTarget): StudioMirStreamsResult =
        try {
            repository.getStreams(target)?.let { StudioMirStreamsResult.Success(it) } ?: StudioMirStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            StudioMirStreamsResult.Failed(error.message)
        }
}
