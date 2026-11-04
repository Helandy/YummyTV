package su.afk.yummy.tv.source.amedia.data.repository

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.reportResolveStep
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
        val vodPage = api.getVodPage(target.vodId)
        reportResolveStep(PlayerStreamResolveStatus.ExtractingLink)
        val masterUrl = parseAmediaVodFile(vodPage) ?: run { reportResolveStep(PlayerStreamResolveStatus.LinkNotFound); return null }
        reportResolveStep(PlayerStreamResolveStatus.LoadingPlaylist)
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
