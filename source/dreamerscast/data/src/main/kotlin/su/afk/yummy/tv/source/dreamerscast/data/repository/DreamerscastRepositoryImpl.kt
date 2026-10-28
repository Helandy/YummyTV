package su.afk.yummy.tv.source.dreamerscast.data.repository

import su.afk.yummy.tv.source.dreamerscast.data.network.DreamerscastApi
import su.afk.yummy.tv.source.dreamerscast.data.utils.dreamerscastHls
import su.afk.yummy.tv.source.dreamerscast.data.utils.dreamerscastPageUrl
import su.afk.yummy.tv.source.dreamerscast.data.utils.dreamerscastPlaylist
import su.afk.yummy.tv.source.dreamerscast.data.utils.dreamerscastQualities
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastStreams
import su.afk.yummy.tv.source.dreamerscast.domain.model.DreamerscastTarget
import su.afk.yummy.tv.source.dreamerscast.domain.repository.DreamerscastRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class DreamerscastRepositoryImpl @Inject constructor(
    private val api: DreamerscastApi,
) : DreamerscastRepository {

    /**
     * Цепочка: страница релиза → обфусцированный `new Playerjs("#2…")` → плейлист серий → HLS-мастер серии → варианты. Ссылки
     * CDN не подписаны и отвечают без заголовков.
     */
    override suspend fun getStreams(target: DreamerscastTarget): DreamerscastStreams? {
        val page = api.getText(dreamerscastPageUrl(target.slug)) ?: return null
        val playlist = dreamerscastPlaylist(page) ?: return null
        val masterUrl = dreamerscastHls(playlist, target.episode) ?: return null
        val master = api.getText(masterUrl) ?: return null
        return DreamerscastStreams(qualities = dreamerscastQualities(master), masterUrl = masterUrl)
    }
}
