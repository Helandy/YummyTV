package su.afk.yummy.tv.source.fenix.data.repository

import su.afk.yummy.tv.source.fenix.data.network.FenixApi
import su.afk.yummy.tv.source.fenix.data.utils.fenixAccessToken
import su.afk.yummy.tv.source.fenix.data.utils.fenixMasterUrl
import su.afk.yummy.tv.source.fenix.data.utils.fenixPlaylist
import su.afk.yummy.tv.source.fenix.data.utils.parseFenixPage
import su.afk.yummy.tv.source.fenix.data.utils.signedFenixMaster
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreams
import su.afk.yummy.tv.source.fenix.domain.model.FenixStreamsResult
import su.afk.yummy.tv.source.fenix.domain.repository.FenixRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class FenixRepositoryImpl @Inject constructor(
    private val api: FenixApi,
) : FenixRepository {

    /**
     * Страница плеера → `FENIX_PLAYLIST` (серия и озвучка) и `FENIX_MEDIA_ACCESS` (токен) → HLS-мастер с токеном в query. Токен
     * живёт ограниченное время, поэтому страница читается при каждом воспроизведении. Гео-блока у Fenix нет.
     */
    override suspend fun getStreams(pageUrl: String, track: String?): FenixStreamsResult {
        val page = parseFenixPage(pageUrl) ?: return FenixStreamsResult.NotFound
        val html = api.getText("https://fenixplay.xyz${page.path}") ?: return FenixStreamsResult.NotFound
        val playlist = fenixPlaylist(html) ?: return FenixStreamsResult.NotFound
        val token = fenixAccessToken(html) ?: return FenixStreamsResult.NotFound
        val master = fenixMasterUrl(playlist, page, track)?.let { signedFenixMaster(it, token) }
            ?: return FenixStreamsResult.NotFound
        return FenixStreamsResult.Success(FenixStreams(masterUrl = master))
    }
}
