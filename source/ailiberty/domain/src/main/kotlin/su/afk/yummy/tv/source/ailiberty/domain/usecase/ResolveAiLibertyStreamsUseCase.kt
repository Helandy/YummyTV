package su.afk.yummy.tv.source.ailiberty.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyStreamsResult
import su.afk.yummy.tv.source.ailiberty.domain.model.AiLibertyTarget
import su.afk.yummy.tv.source.ailiberty.domain.repository.AiLibertyRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в HLS-потоки AiLiberty: подписанные ссылки живут недолго, поэтому страница релиза читается в момент
 * воспроизведения. Отсутствие релиза или серии — [AiLibertyStreamsResult.NotFound], сбой сети — [AiLibertyStreamsResult.Failed].
 */
class ResolveAiLibertyStreamsUseCase @Inject constructor(
    private val repository: AiLibertyRepository,
) {
    suspend operator fun invoke(target: AiLibertyTarget): AiLibertyStreamsResult =
        try {
            repository.getStreams(target)?.let { AiLibertyStreamsResult.Success(it) } ?: AiLibertyStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AiLibertyStreamsResult.Failed(error.message)
        }
}
