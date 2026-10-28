package su.afk.yummy.tv.source.fenix.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import su.afk.yummy.tv.source.fenix.domain.repository.FenixRepository
import javax.inject.Inject

/**
 * Превращает страницу плеера Fenix в HLS-поток: список серий, озвучек и подписанный доступ к медиа отдаёт сама страница, поэтому
 * она читается в момент воспроизведения. Отсутствие серии — [FenixStreamsResult.NotFound], сбой сети — [FenixStreamsResult.Failed].
 */
class ResolveFenixStreamsUseCase @Inject constructor(
    private val repository: FenixRepository,
) {
    suspend operator fun invoke(pageUrl: String, track: String? = null): FenixStreamsResult =
        try {
            repository.getStreams(pageUrl, track)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            FenixStreamsResult.Failed(error.message)
        }
}
