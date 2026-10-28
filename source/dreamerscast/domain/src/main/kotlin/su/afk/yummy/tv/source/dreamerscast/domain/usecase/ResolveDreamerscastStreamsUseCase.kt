package su.afk.yummy.tv.source.dreamerscast.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastStreamsResult
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget
import su.afk.yummy.tv.source.dreamerscast.domain.repository.DreamerscastRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в HLS-потоки Dream Cast: плейлист серий лежит в странице релиза (обфусцирован), поэтому страница и мастер
 * читаются в момент воспроизведения. Отсутствие серии или ссылки — [DreamerscastStreamsResult.NotFound], сбой сети —
 * [DreamerscastStreamsResult.Failed].
 */
class ResolveDreamerscastStreamsUseCase @Inject constructor(
    private val repository: DreamerscastRepository,
) {
    suspend operator fun invoke(target: DreamerscastTarget): DreamerscastStreamsResult =
        try {
            repository.getStreams(target)?.let { DreamerscastStreamsResult.Success(it) } ?: DreamerscastStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            DreamerscastStreamsResult.Failed(error.message)
        }
}
