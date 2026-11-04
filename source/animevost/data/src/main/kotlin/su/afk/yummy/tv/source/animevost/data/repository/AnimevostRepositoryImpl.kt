package su.afk.yummy.tv.source.animevost.data.repository

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.reportResolveStep
import su.afk.yummy.tv.source.animevost.data.network.AnimevostApi
import su.afk.yummy.tv.source.animevost.data.utils.animevostEpisodeId
import su.afk.yummy.tv.source.animevost.data.utils.animevostFrameUrl
import su.afk.yummy.tv.source.animevost.data.utils.animevostPageUrl
import su.afk.yummy.tv.source.animevost.data.utils.animevostQualities
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostStreams
import su.afk.yummy.tv.source.animevost.domain.model.AnimevostTarget
import su.afk.yummy.tv.source.animevost.domain.repository.AnimevostRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AnimevostRepositoryImpl @Inject constructor(
    private val api: AnimevostApi,
) : AnimevostRepository {

    /**
     * Цепочка: страница тайтла → `var data` с id серий → плеер `frame5.php` → прямые mp4 480p/720p. Ссылки подписаны (`md5`,
     * `time`) и живут недолго, поэтому читаются в момент воспроизведения. Плеер закрыт для краулеров в robots.txt, но здесь
     * его открывает сам пользователь, и сервер YummyTV его не читает.
     */
    override suspend fun getStreams(target: AnimevostTarget): AnimevostStreams? {
        val page = api.getText(animevostPageUrl(target.typePath, target.slug)) ?: return null
        reportResolveStep(PlayerStreamResolveStatus.SearchingEpisode)
        val episodeId = animevostEpisodeId(page, target.episode) ?: run { reportResolveStep(PlayerStreamResolveStatus.EpisodeNotFound); return null }
        reportResolveStep(PlayerStreamResolveStatus.ExtractingLink)
        val frame = api.getText(animevostFrameUrl(episodeId)) ?: return null
        val qualities = animevostQualities(frame)
        if (qualities.isEmpty()) {
            reportResolveStep(PlayerStreamResolveStatus.LinkNotFound)
            return null
        }
        return AnimevostStreams(qualities)
    }
}
