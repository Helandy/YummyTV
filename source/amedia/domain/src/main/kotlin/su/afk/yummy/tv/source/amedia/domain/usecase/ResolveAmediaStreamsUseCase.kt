package su.afk.yummy.tv.source.amedia.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreamsResult
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import javax.inject.Inject

/** Превращает адрес серии в HLS-поток Amedia; отсутствие потока — [AmediaStreamsResult.NotFound], сбой сети — [AmediaStreamsResult.Failed]. */
class ResolveAmediaStreamsUseCase @Inject constructor(
    private val repository: AmediaRepository,
) {
    suspend operator fun invoke(target: AmediaTarget): AmediaStreamsResult =
        try {
            repository.getStreams(target)?.let { AmediaStreamsResult.Success(it) } ?: AmediaStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AmediaStreamsResult.Failed(error.message)
        }
}
