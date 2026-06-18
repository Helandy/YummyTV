package su.afk.yummy.tv.core.storage.watchprogress

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RemoteContinueWatchingStore(
    private val dao: RemoteContinueWatchingDao,
    private val watchProgressDao: WatchProgressDao,
) {

    fun observe(
        accountKey: String,
        language: String,
    ): Flow<List<WatchProgressEntry>> =
        dao.observe(accountKey, language)
            .map { entries -> entries.map { it.toWatchProgressEntry() } }

    suspend fun get(
        accountKey: String,
        language: String,
    ): List<WatchProgressEntry> =
        dao.getAll(accountKey, language)
            .map { it.toWatchProgressEntry() }

    suspend fun saveRemoteContinueWatching(
        accountKey: String,
        language: String,
        entries: List<WatchProgressEntry>,
        limit: Int = DEFAULT_LIMIT,
    ) {
        if (entries.isEmpty()) return
        val suppressionTimestamps = watchProgressDao
            .suppressions()
            .associate { it.animeId to it.suppressedAt }
        entries
            .filter { it.animeId > 0 }
            .filterNot { entry ->
                suppressionTimestamps[entry.animeId]?.let { it >= entry.updatedAt } == true
            }
            .forEach { entry ->
                val targetKey = entry.remoteTargetKey() ?: return@forEach
                val existing = dao.get(
                    accountKey = accountKey,
                    language = language,
                    animeId = entry.animeId,
                    targetKey = targetKey,
                )
                if (existing != null && existing.updatedAt > entry.updatedAt) return@forEach
                dao.save(
                    entry.toRemoteContinueWatchingEntry(
                        accountKey = accountKey,
                        language = language,
                        targetKey = targetKey,
                        existing = existing,
                    )
                )
            }
        dao.prune(accountKey = accountKey, language = language, limit = limit)
    }

    suspend fun deleteByAnimeId(animeId: Int) {
        dao.deleteByAnimeId(animeId)
    }

    private fun WatchProgressEntry.toRemoteContinueWatchingEntry(
        accountKey: String,
        language: String,
        targetKey: String,
        existing: RemoteContinueWatchingEntry?,
    ): RemoteContinueWatchingEntry =
        RemoteContinueWatchingEntry(
            accountKey = accountKey,
            language = language,
            animeId = animeId,
            targetKey = targetKey,
            episode = episode.ifBlank { existing?.episode.orEmpty() },
            videoId = videoId.takeIf { it > 0 } ?: existing?.videoId ?: 0,
            episodeUrl = episodeUrl.ifBlank { existing?.episodeUrl.orEmpty() },
            positionMs = positionMs,
            durationMs = durationMs.takeIf { it > 0L } ?: existing?.durationMs ?: 0L,
            updatedAt = updatedAt,
            animeTitle = animeTitle.ifBlank { existing?.animeTitle.orEmpty() },
            posterUrl = posterUrl.ifBlank { existing?.posterUrl.orEmpty() },
            playerName = playerName.ifBlank { existing?.playerName.orEmpty() },
            dubbing = dubbing.ifBlank { existing?.dubbing.orEmpty() },
            screenshotUrl = screenshotUrl.ifBlank { existing?.screenshotUrl.orEmpty() },
        )

    private fun RemoteContinueWatchingEntry.toWatchProgressEntry(): WatchProgressEntry =
        WatchProgressEntry(
            animeId = animeId,
            episode = episode,
            videoId = videoId,
            episodeUrl = episodeUrl,
            positionMs = positionMs,
            durationMs = durationMs,
            updatedAt = updatedAt,
            animeTitle = animeTitle,
            posterUrl = posterUrl,
            playerName = playerName,
            dubbing = dubbing,
            screenshotUrl = screenshotUrl,
        )

    private fun WatchProgressEntry.remoteTargetKey(): String? =
        when {
            videoId > 0 -> "video:$videoId"
            episodeUrl.isNotBlank() -> "url:$episodeUrl"
            episode.isNotBlank() -> "episode:${episode.trim().lowercase()}"
            else -> null
        }

    private companion object {
        const val DEFAULT_LIMIT = 100
    }
}
