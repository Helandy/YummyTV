package su.afk.yummy.tv.source.animebesst.domain.usecase

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstStreamsResult
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget
import su.afk.yummy.tv.source.animebesst.domain.repository.AnimebesstRepository
import javax.inject.Inject

/**
 * Превращает адрес серии в HLS-потоки Animebesst: страница тайтла (с запасными зеркалами), страница плеера серии и мастер читаются
 * в момент воспроизведения. Отсутствие серии или файла — [AnimebesstStreamsResult.NotFound], сбой сети — [AnimebesstStreamsResult.Failed].
 */
class ResolveAnimebesstStreamsUseCase @Inject constructor(
    private val repository: AnimebesstRepository,
) {
    suspend operator fun invoke(target: AnimebesstTarget): AnimebesstStreamsResult =
        try {
            repository.getStreams(target)?.let { AnimebesstStreamsResult.Success(it) } ?: AnimebesstStreamsResult.NotFound
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            AnimebesstStreamsResult.Failed(error.message)
        }
}
