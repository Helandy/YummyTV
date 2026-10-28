package su.afk.yummy.tv.source.anidub.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.anidub.domain.model.AniDubStreamsResult
import su.afk.yummy.tv.source.anidub.domain.model.AniDubTarget
import su.afk.yummy.tv.source.anidub.domain.repository.AniDubRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в HLS-потоки AniDub: подписанные ссылки на CDN живут около двух часов, поэтому страница тайтла и плейлист
 * плеера читаются в момент воспроизведения. Отсутствие тайтла, плеера или серии — [AniDubStreamsResult.NotFound], сбой сети —
 * [AniDubStreamsResult.Failed].
 */
class ResolveAniDubStreamsUseCase @Inject constructor(
    private val repository: AniDubRepository,
) {
    suspend operator fun invoke(target: AniDubTarget): AniDubStreamsResult =
        try {
            repository.getStreams(target)?.let { AniDubStreamsResult.Success(it) } ?: AniDubStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AniDubStreamsResult.Failed(error.message)
        }
}
