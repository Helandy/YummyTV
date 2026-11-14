package su.afk.yummy.tv.source.anime4k.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kApiException
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kStreamsResult
import su.afk.yummy.tv.source.anime4k.domain.model.Anime4kTarget
import su.afk.yummy.tv.source.anime4k.domain.repository.Anime4kRepository
import javax.inject.Inject

/**
 * Открывает 4K-озвучку: запрашивает у сервера ссылки на файлы выбранной серии и озвучки. Ошибки сервера и сети
 * превращаются в варианты [Anime4kStreamsResult], наружу уходит только отмена.
 */
class ResolveAnime4kStreamsUseCase @Inject constructor(
    private val repository: Anime4kRepository,
) {
    suspend operator fun invoke(target: Anime4kTarget): Anime4kStreamsResult =
        try {
            Anime4kStreamsResult.Success(repository.getStreams(target))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Anime4kApiException.RateLimited) {
            Anime4kStreamsResult.RateLimited
        } catch (_: Anime4kApiException.Rejected) {
            Anime4kStreamsResult.Rejected
        } catch (_: Anime4kApiException.NotFound) {
            Anime4kStreamsResult.NotFound
        } catch (error: Exception) {
            Anime4kStreamsResult.Failed(error.message)
        }
}
