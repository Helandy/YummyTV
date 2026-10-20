package su.afk.yummy.tv.source.anistar.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.anistar.domain.model.AnistarStreamsResult
import su.afk.yummy.tv.source.anistar.domain.model.AnistarTarget
import su.afk.yummy.tv.source.anistar.domain.repository.AnistarRepository
import javax.inject.Inject

/** Превращает адрес серии в HLS-потоки AniStar; отсутствие серии — [AnistarStreamsResult.NotFound], сбой сети — [AnistarStreamsResult.Failed]. */
class ResolveAnistarStreamsUseCase @Inject constructor(
    private val repository: AnistarRepository,
) {
    suspend operator fun invoke(target: AnistarTarget): AnistarStreamsResult =
        try {
            repository.getStreams(target)?.takeIf { it.hlsByQuality.isNotEmpty() }
                ?.let { AnistarStreamsResult.Success(it) } ?: AnistarStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AnistarStreamsResult.Failed(error.message)
        }
}
