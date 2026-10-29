package su.afk.yummy.tv.source.fenix.data.repository

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import su.afk.yummy.tv.source.fenix.data.network.FenixApi
import su.afk.yummy.tv.source.fenix.data.utils.fenixAccessToken
import su.afk.yummy.tv.source.fenix.data.utils.fenixEdgeAccess
import su.afk.yummy.tv.source.fenix.data.utils.fenixMasterUrl
import su.afk.yummy.tv.source.fenix.data.utils.fenixPlaylist
import su.afk.yummy.tv.source.fenix.data.utils.parseFenixPage
import su.afk.yummy.tv.source.fenix.data.utils.signedFenixEdgeMaster
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
     * живёт ограниченное время, поэтому страница читается при каждом воспроизведении. Гео-блока у Fenix нет. Основной
     * медиа-хост бывает недоступен или медленен: у страницы есть запасной хост (`edge`), и тот же мастер
     * проверяется на обоих хостах параллельно, см. [firstReachable].
     */
    override suspend fun getStreams(pageUrl: String, track: String?): FenixStreamsResult {
        val page = parseFenixPage(pageUrl) ?: return FenixStreamsResult.NotFound
        val html = api.getText("https://fenixplay.xyz${page.path}") ?: return FenixStreamsResult.NotFound
        val playlist = fenixPlaylist(html) ?: return FenixStreamsResult.NotFound
        val token = fenixAccessToken(html) ?: return FenixStreamsResult.NotFound
        val master = fenixMasterUrl(playlist, page, track)?.let { signedFenixMaster(it, token) }
            ?: return FenixStreamsResult.NotFound
        val edgeMaster = fenixEdgeAccess(html)?.let { signedFenixEdgeMaster(master, it) }
            ?: return FenixStreamsResult.Success(FenixStreams(masterUrl = master))
        return FenixStreamsResult.Success(FenixStreams(masterUrl = firstReachable(master, edgeMaster)))
    }

    /**
     * Основной хост может отвечать медленно (даже если мастер ещё проходит, плейлисты и сегменты потом встают), поэтому оба
     * адреса пробуются параллельно и играет тот, что ответил первым; основной выигрывает при равенстве. Если не ответил ни
     * один — основной, как раньше.
     */
    private suspend fun firstReachable(main: String, edge: String): String = coroutineScope {
        val results = Channel<String?>(Channel.UNLIMITED)
        val probes = listOf(main, edge).map { url ->
            launch { results.send(url.takeIf { api.isReachable(it, FIRST_BYTE_TIMEOUT_MS) }) }
        }
        var winner: String? = null
        repeat(probes.size) { if (winner == null) winner = results.receive() }
        probes.forEach { it.cancel() }
        winner ?: main
    }

    private companion object {
        const val FIRST_BYTE_TIMEOUT_MS = 4_000L
    }
}
