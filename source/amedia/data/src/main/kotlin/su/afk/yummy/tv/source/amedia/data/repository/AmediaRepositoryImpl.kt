package su.afk.yummy.tv.source.amedia.data.repository

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.amedia.data.network.AmediaApi
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaMasterPlaylist
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaVodFile
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AmediaRepositoryImpl @Inject constructor(
    private val api: AmediaApi,
) : AmediaRepository {

    /** Мастер-плейлист не подписан, но берётся заново при каждом запуске серии; недоступные варианты качества не мешают воспроизведению. */
    override suspend fun getStreams(target: AmediaTarget): AmediaStreams? {
        val masterUrl = parseAmediaVodFile(api.getVodPage(target.vodId)) ?: return null
        val variants = try {
            parseAmediaMasterPlaylist(api.getPlaylist(masterUrl), masterUrl)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            emptyMap()
        }
        return AmediaStreams(masterUrl, variants)
    }
}
