package su.afk.yummy.tv.source.anilibria.data.repository

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import su.afk.yummy.tv.source.anilibria.data.mapper.AniLibriaMapper
import su.afk.yummy.tv.source.anilibria.data.network.AniLibriaApi
import su.afk.yummy.tv.source.anilibria.domain.model.AniLibriaRelease
import su.afk.yummy.tv.source.anilibria.domain.repository.AniLibriaRepository
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AniLibriaRepositoryImpl @Inject constructor(
    private val api: AniLibriaApi,
) : AniLibriaRepository {

    private data class Cached(val release: AniLibriaRelease?, val fetchedAtMs: Long)

    private val byReleaseId = ConcurrentHashMap<Int, Cached>()

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
        const val RETRY_DELAY_MS = 400L
    }
}
