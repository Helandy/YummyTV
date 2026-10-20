package su.afk.yummy.tv.source.aniboom.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomStreamsResult
import su.afk.yummy.tv.source.aniboom.domain.model.AniboomTarget
import su.afk.yummy.tv.source.aniboom.domain.repository.AniboomRepository
import javax.inject.Inject

/** Превращает адрес серии в HLS-поток AniBoom; отсутствие серии или озвучки — [AniboomStreamsResult.NotFound], сбой сети — [AniboomStreamsResult.Failed]. */
class ResolveAniboomStreamsUseCase @Inject constructor(
    private val repository: AniboomRepository,
) {
    suspend operator fun invoke(target: AniboomTarget): AniboomStreamsResult =
        try {
            repository.getStreams(target)?.let { AniboomStreamsResult.Success(it) } ?: AniboomStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AniboomStreamsResult.Failed(error.message)
        }
}
