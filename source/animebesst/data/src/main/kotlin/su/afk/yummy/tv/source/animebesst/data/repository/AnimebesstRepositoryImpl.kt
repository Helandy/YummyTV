package su.afk.yummy.tv.source.animebesst.data.repository

import su.afk.yummy.tv.source.animebesst.data.network.AnimebesstApi
import su.afk.yummy.tv.source.animebesst.data.utils.animebesstMaster
import su.afk.yummy.tv.source.animebesst.data.utils.animebesstPageUrl
import su.afk.yummy.tv.source.animebesst.data.utils.animebesstPlayerUrl
import su.afk.yummy.tv.source.animebesst.data.utils.animebesstQualities
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstStreams
import su.afk.yummy.tv.source.animebesst.domain.model.AnimebesstTarget
import su.afk.yummy.tv.source.animebesst.domain.repository.AnimebesstRepository
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException

@Singleton
internal class AnimebesstRepositoryImpl @Inject constructor(
    private val api: AnimebesstApi,
) : AnimebesstRepository {

    /**
     * Цепочка: страница тайтла (основной хост, при сбое сети зеркала по очереди) → `var videoList` → страница плеера серии →
     * `file:` в её Playerjs → HLS-мастер → варианты с относительными адресами. Плеер живёт на отдельном домене и сайтовые
     * блокировки не затрагивает; ссылки CDN не подписаны.
     */
    override suspend fun getStreams(target: AnimebesstTarget): AnimebesstStreams? {
        val page = page(target) ?: return null
        val playerUrl = animebesstPlayerUrl(page, target.episode) ?: return null
        val masterUrl = animebesstMaster(api.getText(playerUrl) ?: return null) ?: return null
        val master = api.getText(masterUrl) ?: return null
        return AnimebesstStreams(qualities = animebesstQualities(master, masterUrl), masterUrl = masterUrl)
    }

    /**
     * Первая открывшаяся страница. 404 хоста не считается ответом «нет тайтла» сразу, пока есть другие хосты: зеркало может
     * отдать то, чего нет на основном. Если все хосты упали по сети, ошибка последнего уходит наверх.
     */
    private suspend fun page(target: AnimebesstTarget): String? {
        var failure: IOException? = null
        for (host in target.hosts) {
            try {
                api.getText(animebesstPageUrl(host, target.slug))?.let { return it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                failure = e
            }
        }
        failure?.let { throw it }
        return null
    }
}
