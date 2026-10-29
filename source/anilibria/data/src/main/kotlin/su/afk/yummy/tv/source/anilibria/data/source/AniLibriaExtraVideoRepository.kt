package su.afk.yummy.tv.source.anilibria.data.source

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkipSegment
import su.afk.yummy.tv.core.model.anime.AnimeVideoSkips
import su.afk.yummy.tv.domain.anime.repository.ExtraVideoRepository
import su.afk.yummy.tv.source.anilibria.data.mapper.AniLibriaMapper
import su.afk.yummy.tv.source.anilibria.data.utils.toSourceUrl
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaTarget
import su.afk.yummy.tv.source.availability.domain.model.AvailabilityLookup
import su.afk.yummy.tv.source.availability.domain.usecase.FindSourceAvailabilityUseCase
import javax.inject.Inject

/**
 * Добавляет балансер «AniLibria» с родными потоками релизов (озвучка и новый дубляж). Показывается только внутри озвучки AniLibria,
 * которая уже есть у Yani ([requiresMatchingDubbing]). Ссылок на файлы здесь нет.
 */
internal class AniLibriaExtraVideoRepository @Inject constructor(
    private val findAvailability: FindSourceAvailabilityUseCase,
) : ExtraVideoRepository {

    override val sourceId: String = SOURCE_ID

    override val requiresMatchingDubbing: Boolean = true

    override suspend fun videos(details: AnimeDetails): List<AnimeVideo> {
        val malId = details.malId ?: return emptyList()
        val lookup = AvailabilityLookup(
            malId = malId,
            title = details.title,
            otherTitles = details.otherTitles,
            year = details.year,
            type = details.type,
            episodes = details.episodes?.count,
        )
        // Релизы (озвучка и новый дубляж) приходят вариантами общей ручки наличия, AniLibria здесь не опрашивается.
        val releases = findAvailability(SOURCE_ID, lookup)?.variants.orEmpty().mapNotNull { AniLibriaMapper.toRelease(malId, it) }

        return releases.flatMap { release ->
            // Имена как у Yani: у тайтла рядом стоят «Озвучка AniLibria» и «Дубляж AniLibria», по голому «AniLibria» их не различить.
            val dubbing = if (release.isDubbing) DUBBING_NAME_DUB else DUBBING_NAME_VOICE
            release.episodes.map { episode ->
                val url = AniLibriaTarget(release.id, episode.ordinal).toSourceUrl()
                AnimeVideo(
                    // Отрицательный id: серверу Yani такие видео неизвестны, прогресс остаётся локальным.
                    id = -(url.hashCode() and Int.MAX_VALUE) - 1,
                    episode = episode.ordinal.toString(),
                    dubbing = dubbing,
                    player = PLAYER_NAME,
                    playerId = null,
                    iframeUrl = url,
                    durationSeconds = episode.durationSec,
                    skips = AnimeVideoSkips(
                        opening = episode.opening?.let { AnimeVideoSkipSegment(it.startSec * 1000L, it.endSec * 1000L) },
                    ),
                )
            }
        }
    }

    private companion object {
        const val SOURCE_ID = "anilibria"
        const val PLAYER_NAME = "AniLibria"
        const val DUBBING_NAME_VOICE = "Озвучка AniLibria"
        const val DUBBING_NAME_DUB = "Дубляж AniLibria"
    }
}
