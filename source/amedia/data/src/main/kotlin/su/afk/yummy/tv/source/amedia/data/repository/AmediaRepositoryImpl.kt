package su.afk.yummy.tv.source.amedia.data.repository

import kotlinx.coroutines.CancellationException
import su.afk.yummy.tv.source.amedia.data.mapper.AmediaMapper
import su.afk.yummy.tv.source.amedia.data.network.AmediaApi
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaMasterPlaylist
import su.afk.yummy.tv.source.amedia.data.utils.parseAmediaVodFile
import su.afk.yummy.tv.source.amedia.domain.model.AmediaLookup
import su.afk.yummy.tv.source.amedia.domain.model.AmediaStreams
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTarget
import su.afk.yummy.tv.source.amedia.domain.model.AmediaTitle
import su.afk.yummy.tv.source.amedia.domain.repository.AmediaRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AmediaRepositoryImpl @Inject constructor(
    private val api: AmediaApi,
) : AmediaRepository {

    private data class CachedTitle(val title: AmediaTitle?, val fetchedAtMs: Long)

    private val byMalId = ConcurrentHashMap<Int, CachedTitle>()

    override suspend fun findTitle(lookup: AmediaLookup): AmediaTitle? {
        val now = System.currentTimeMillis()
        byMalId[lookup.malId]?.takeIf { now - it.fetchedAtMs < TITLE_TTL_MS }?.let { return it.title }

        // Сбой сети в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val dto = api.findTitle(AmediaMapper.toRequest(lookup))
        // Тайтл есть, а серии сервер не прочитал: это сбой, а не «серий нет» — пустой ответ не кэшируем.
        if (dto.available && dto.episodes == null) throw IOException("Amedia API: сервер не прочитал серии")
        val title = AmediaMapper.toTitle(dto)
        byMalId[lookup.malId] = CachedTitle(title, now)
        return title
    }

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

    private companion object {
        // Короткий срок: вместе с тайтлом приходят серии, а новая серия не должна ждать дольше пары минут сверх серверного кэша.
        const val TITLE_TTL_MS = 5 * 60 * 1000L
    }
}
