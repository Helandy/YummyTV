package su.afk.yummy.tv.source.ashdi.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.ashdi.domain.model.AshdiStreamsResult
import su.afk.yummy.tv.source.ashdi.domain.repository.AshdiRepository
import javax.inject.Inject

/**
 * Превращает страницу плеера Ashdi в HLS-потоки: ссылку на мастер отдаёт сама страница, поэтому она читается в момент
 * воспроизведения. Отсутствие файла — [AshdiStreamsResult.NotFound], закрытый для страны плеер — [AshdiStreamsResult.RegionBlocked],
 * сбой сети — [AshdiStreamsResult.Failed].
 */
class ResolveAshdiStreamsUseCase @Inject constructor(
    private val repository: AshdiRepository,
) {
    suspend operator fun invoke(pageUrl: String): AshdiStreamsResult =
        try {
            repository.getStreams(pageUrl)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AshdiStreamsResult.Failed(error.message)
        }
}
