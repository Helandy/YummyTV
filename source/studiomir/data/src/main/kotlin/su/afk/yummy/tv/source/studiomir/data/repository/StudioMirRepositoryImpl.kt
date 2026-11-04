package su.afk.yummy.tv.source.studiomir.data.repository

import su.afk.yummy.tv.source.studiomir.data.network.StudioMirApi
import su.afk.yummy.tv.source.studiomir.data.utils.studioMirEpisodeQualities
import su.afk.yummy.tv.source.studiomir.data.utils.studioMirPageUrl
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirStreams
import su.afk.yummy.tv.source.studiomir.domain.model.StudioMirTarget
import su.afk.yummy.tv.source.studiomir.domain.repository.StudioMirRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class StudioMirRepositoryImpl @Inject constructor(
    private val api: StudioMirApi,
) : StudioMirRepository {

    /** Страница тайтла → плейлист плеера в HTML → HLS-варианты нужной серии. Ссылки не подписаны, мастера у серии нет. */
    override suspend fun getStreams(target: StudioMirTarget): StudioMirStreams? {
        val page = api.getText(studioMirPageUrl(target.slug)) ?: return null
        val qualities = studioMirEpisodeQualities(page, target.episode)
        val best = qualities.values.lastOrNull() ?: return null
        return StudioMirStreams(qualities = qualities, masterUrl = best)
    }
}
