package su.afk.yummy.tv.source.moon.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.moon.domain.model.MoonStreamsResult
import su.afk.yummy.tv.source.moon.domain.repository.MoonRepository
import javax.inject.Inject

/**
 * Превращает страницу плеера Moon в поток: ссылка на файл зашита в скрипт страницы, поэтому она читается в момент воспроизведения.
 * Отсутствие страницы — [MoonStreamsResult.NotFound], сбой сети и изменившаяся страница — [MoonStreamsResult.Failed].
 */
class ResolveMoonStreamsUseCase @Inject constructor(
    private val repository: MoonRepository,
) {
    suspend operator fun invoke(pageUrl: String): MoonStreamsResult =
        try {
            repository.getStreams(pageUrl)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            MoonStreamsResult.Failed(error.message)
        }
}
