package su.afk.yummy.tv.source.anilibria.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.model.anime.utils.prioritizeAnimeTitles
import su.afk.yummy.tv.source.anilibria.data.dto.AniLibriaLookupRequestDto
import su.afk.yummy.tv.source.anilibria.data.mapper.AniLibriaMapper
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AniLibriaRepositoryImpl @Inject constructor(
    private val api: AniLibriaApi,
) : AniLibriaRepository {

    private data class Cached(val release: AniLibriaRelease?, val fetchedAtMs: Long)

    private data class CachedList(val releases: List<AniLibriaRelease>, val fetchedAtMs: Long)

    private val byMalId = ConcurrentHashMap<Int, CachedList>()
    private val byReleaseId = ConcurrentHashMap<Int, Cached>()

    override suspend fun findReleases(malId: Int, titles: List<String>, year: Int?): List<AniLibriaRelease> {
        val now = System.currentTimeMillis()
        byMalId[malId]?.let { cached ->
            val ttl = if (cached.releases.isEmpty()) EMPTY_TTL_MS else TTL_MS
            if (now - cached.fetchedAtMs < ttl) return cached.releases
        }

        // Только копия каталога на сервере YummyTV: AniLibria не опрашивается, пока пользователь не запустил серию. Сбой сервера
        // в кэш не попадает: исключение уйдёт наверх, источник просто не появится в этот раз.
        val sorted = prioritizeAnimeTitles("", titles, MAX_TITLES)
        val title = sorted.firstOrNull()
        val releases = if (title == null) {
            emptyList()
        } else {
            val response = api.findReleases(AniLibriaLookupRequestDto(malId, title, sorted.drop(1), year))
            // Релиз найден, а серии сервер ещё не прочитал: это не «релизов нет», пустой ответ не кэшируем.
            if (response.releases.size < response.releaseIds.size) throw IOException("AniLibria catalog: серии релизов ещё не прочитаны")
            response.releases.map { AniLibriaMapper.toRelease(malId, it) }
        }
        byMalId[malId] = CachedList(releases, System.currentTimeMillis())
        return releases
    }

    /** Релиз со ссылками на потоки: запрашивается у AniLibria только при запуске серии. */
    override suspend fun getRelease(releaseId: Int): AniLibriaRelease? {
        val now = System.currentTimeMillis()
        byReleaseId[releaseId]?.takeIf { now - it.fetchedAtMs < TTL_MS }?.let { return it.release }

        val dto = withRetry { api.getRelease(releaseId) }
        // У части релизов MAL id не заполнен: они находятся по названию и году, поэтому id релиза здесь не обязателен.
        val release = dto?.let { AniLibriaMapper.toRelease(it.mal?.id ?: 0, it) }
        byReleaseId[releaseId] = Cached(release, System.currentTimeMillis())
        return release
    }

    /** Один повтор после короткой паузы: API периодически отвечает 5xx/таймаутом, а следующая попытка проходит. */
    private suspend fun <T> withRetry(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        delay(RETRY_DELAY_MS)
        block()
    }

    private companion object {
        const val TTL_MS = 30 * 60 * 1000L
        const val EMPTY_TTL_MS = 5 * 60 * 1000L
        const val RETRY_DELAY_MS = 400L
        const val MAX_TITLES = 30
    }
}
