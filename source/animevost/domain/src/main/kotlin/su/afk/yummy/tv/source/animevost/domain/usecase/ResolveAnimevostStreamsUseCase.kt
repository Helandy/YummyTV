package su.afk.yummy.tv.source.animevost.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostStreamsResult
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget
import su.afk.yummy.tv.source.animevost.domain.repository.AnimevostRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в прямые mp4 AnimeVost: страница тайтла и плеер серии читаются в момент воспроизведения, потому что
 * ссылки подписаны и живут недолго. Отсутствие серии или ссылок — [AnimevostStreamsResult.NotFound], сбой сети —
 * [AnimevostStreamsResult.Failed].
 */
class ResolveAnimevostStreamsUseCase @Inject constructor(
    private val repository: AnimevostRepository,
) {
    suspend operator fun invoke(target: AnimevostTarget): AnimevostStreamsResult =
        try {
            repository.getStreams(target)?.let { AnimevostStreamsResult.Success(it) } ?: AnimevostStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AnimevostStreamsResult.Failed(error.message)
        }
}
