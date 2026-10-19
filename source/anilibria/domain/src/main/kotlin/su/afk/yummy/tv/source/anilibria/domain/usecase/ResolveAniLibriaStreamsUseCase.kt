package su.afk.yummy.tv.source.anilibria.domain.usecase

import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaStreams
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaStreamsResult
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaTarget
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import javax.inject.Inject

/** Превращает адрес серии в HLS-потоки по качествам (480/720/1080); серия без единой ссылки считается ненайденной. */
class ResolveAniLibriaStreamsUseCase @Inject constructor(
    private val repository: AniLibriaRepository,
) {
    suspend operator fun invoke(target: AniLibriaTarget): AniLibriaStreamsResult {
        val release = repository.getRelease(target.releaseId) ?: return AniLibriaStreamsResult.NotFound
        val episode = release.episodes.firstOrNull { it.ordinal == target.ordinal }
            ?: return AniLibriaStreamsResult.NotFound
        val files = listOf(480 to episode.hls480, 720 to episode.hls720, 1080 to episode.hls1080)
            .mapNotNull { (quality, url) -> url?.takeIf { it.isNotBlank() }?.let { quality to it } }
            .toMap()
        return if (files.isEmpty()) AniLibriaStreamsResult.NotFound
        else AniLibriaStreamsResult.Success(AniLibriaStreams(files))
    }
}
