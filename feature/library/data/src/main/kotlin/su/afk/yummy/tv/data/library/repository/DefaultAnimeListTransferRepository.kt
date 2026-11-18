package su.afk.yummy.tv.data.library.repository

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.error.isRetryableMutationFailure
import su.afk.yummy.tv.core.preferences.api.YaniAccountSettingsStore
import su.afk.yummy.tv.core.storage.library.LibraryStorage
import su.afk.yummy.tv.core.storage.watchprogress.WatchProgressStorage
import su.afk.yummy.tv.data.library.dto.YaniTransferLookupAnimeDto
import su.afk.yummy.tv.data.library.mapper.externalIdFor
import su.afk.yummy.tv.data.library.mapper.toExportableLibraryAnime
import su.afk.yummy.tv.data.library.mapper.toResolvedExternalAnime
import su.afk.yummy.tv.data.library.network.YaniListTransferApi
import su.afk.yummy.tv.data.library.utils.countWatchedEpisodesByAnimeId
import su.afk.yummy.tv.data.library.utils.formatShikimoriJsonAnimeList
import su.afk.yummy.tv.data.library.utils.parseExternalListBytes
import su.afk.yummy.tv.domain.library.model.AnimeListTransferStage
import su.afk.yummy.tv.domain.library.model.AnimeListTransferStageException
import su.afk.yummy.tv.domain.library.model.ExportableLibraryAnime
import su.afk.yummy.tv.domain.library.model.ExternalIdLookupResult
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ExternalListParseError
import su.afk.yummy.tv.domain.library.model.ExternalListParseException
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.model.ResolvedExternalAnime
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import su.afk.yummy.tv.domain.library.utils.MAX_IMPORT_RAW_BYTES
import su.afk.yummy.tv.domain.library.utils.computeImportRetryDelayMs
import su.afk.yummy.tv.domain.library.utils.formatMalXmlAnimeList
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject

class DefaultAnimeListTransferRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val api: YaniListTransferApi,
    private val settingsStore: YaniAccountSettingsStore,
    private val libraryStorage: LibraryStorage,
    private val watchProgressStorage: WatchProgressStorage,
    private val clock: AppClock,
) : AnimeListTransferRepository {

    override suspend fun currentUserId(): Int = settingsStore.yaniUserId.first()

    override suspend fun currentNickname(): String = settingsStore.yaniNickname.first()

    override suspend fun parseExternalListUri(uriString: String): ParsedExternalList =
        withContext(Dispatchers.IO) {
            val uri = uriString.toUri()
            val input = context.contentResolver.openInputStream(uri)
                ?: throw IOException("Cannot open input stream for $uri")
            val bytes = input.use { readBytesBounded(it, MAX_IMPORT_RAW_BYTES) }
            parseExternalListBytes(bytes)
        }

    override fun formatExternalList(
        format: ExternalListFormat,
        userName: String,
        entries: List<ExternalListEntry>,
    ): String =
        when (format) {
            ExternalListFormat.MAL_XML -> formatMalXmlAnimeList(userName = userName, entries = entries)
            ExternalListFormat.SHIKIMORI_JSON -> formatShikimoriJsonAnimeList(entries)
        }

    override suspend fun writeExportToUri(uriString: String, content: String) =
        withContext(Dispatchers.IO) {
            val uri = uriString.toUri()
            val resolver = context.contentResolver
            val stream = runCatching { resolver.openOutputStream(uri, "wt") }.getOrNull()
                ?: resolver.openOutputStream(uri)
                ?: throw IOException("Cannot open output stream for $uri")
            stream.bufferedWriter(Charsets.UTF_8).use { writer -> writer.write(content) }
        }

    override suspend fun saveExportToStorage(
        fileName: String,
        mimeType: String,
        content: String,
    ): String = withContext(Dispatchers.IO) {
        val bytes = content.toByteArray(Charsets.UTF_8)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: throw IOException("Cannot insert export into MediaStore Downloads")
            try {
                val output = resolver.openOutputStream(uri)
                    ?: throw IOException("Cannot open MediaStore output stream for $uri")
                output.use { it.write(bytes) }
            } catch (error: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw error
            }
            "${Environment.DIRECTORY_DOWNLOADS}/$fileName"
        } else {
            val dir = context.getExternalFilesDir(null)
                ?: throw IOException("External storage directory is unavailable")
            File(dir, fileName).also { it.writeBytes(bytes) }.absolutePath
        }
    }

    override suspend fun createShareUri(fileName: String, content: String): String =
        withContext(Dispatchers.IO) {
            val exportDir = File(context.cacheDir, SHARE_EXPORT_DIR).apply {
                deleteRecursively()
                mkdirs()
            }
            val file = File(exportDir, fileName).also {
                it.writeText(content, Charsets.UTF_8)
            }
            // Переиспользуем единственный FileProvider приложения из AndroidManifest.xml:
            // исторически он зарегистрирован с authority `${applicationId}.logs` и путями из
            // `app/src/main/res/xml/log_file_paths.xml`, где отдельно объявлен подкаталог `list_exports/`.
            FileProvider.getUriForFile(context, "${context.packageName}$SHARE_PROVIDER_SUFFIX", file)
                .toString()
        }

    override suspend fun loadExportableLibrary(): List<ExportableLibraryAnime> =
        withContext(Dispatchers.IO) {
            val userId = currentUserId()
            if (userId > 0) {
                val remoteItems = runSuspendCatching {
                    fetchWithRetry { api.getAllUserLists(userId) }
                }.getOrElse { error ->
                    throw AnimeListTransferStageException(
                        stage = AnimeListTransferStage.LOAD_USER_LIBRARY,
                        cause = error,
                    )
                }
                return@withContext remoteItems.mapNotNull { it.toExportableLibraryAnime() }
            }

            val localEntries = libraryStorage.getAll()
            if (localEntries.isEmpty()) return@withContext emptyList()

            val animeIds = localEntries.map { it.animeId }.filter { it > 0 }.distinct()
            val lookupByAnimeId = linkedMapOf<Int, YaniTransferLookupAnimeDto>()
            val chunks = animeIds.chunked(LOOKUP_BATCH_SIZE)
            chunks.forEachIndexed { index, chunk ->
                currentCoroutineContext().ensureActive()
                if (index > 0) delay(BATCH_LOOKUP_DELAY_MS)
                val dtos = runSuspendCatching {
                    fetchAllPages { limit, offset ->
                        api.lookupByAnimeIds(ids = chunk, limit = limit, offset = offset)
                    }
                }.getOrElse { error ->
                    throw AnimeListTransferStageException(
                        stage = AnimeListTransferStage.ENRICH_GUEST_LIBRARY,
                        cause = error,
                        completedBatches = index,
                        totalBatches = chunks.size,
                    )
                }
                dtos.forEach { dto ->
                    val id = dto.animeId?.takeIf { it > 0 } ?: return@forEach
                    lookupByAnimeId.putIfAbsent(id, dto)
                }
            }

            localEntries.map { entry -> entry.toExportableLibraryAnime(lookupByAnimeId[entry.animeId]) }
        }

    override suspend fun loadLocalWatchedEpisodeCounts(): Map<Int, Int> =
        withContext(Dispatchers.IO) {
            watchProgressStorage.watchedProgress().countWatchedEpisodesByAnimeId()
        }

    override suspend fun markEpisodesWatchedLocally(
        animeId: Int,
        animeTitle: String,
        posterUrl: String,
        watchedEpisodes: Int,
    ) = withContext(Dispatchers.IO) {
        val capped = watchedEpisodes.coerceIn(0, MAX_LOCAL_IMPORT_EPISODES)
        if (animeId <= 0 || capped == 0) return@withContext
        val now = clock.nowMillis()
        for (episodeNumber in 1..capped) {
            currentCoroutineContext().ensureActive()
            val episode = episodeNumber.toString()
            if (watchProgressStorage.get(animeId, episode) != null) continue
            watchProgressStorage.save(
                animeId = animeId,
                episode = episode,
                videoId = 0,
                episodeUrl = "",
                positionMs = SYNTHETIC_WATCHED_DURATION_MS,
                durationMs = SYNTHETIC_WATCHED_DURATION_MS,
                updatedAt = now,
                animeTitle = animeTitle,
                posterUrl = posterUrl,
            )
        }
    }

    override suspend fun resolveByExternalIds(
        format: ExternalListFormat,
        externalIds: List<Int>,
        onBatchProgress: suspend (processedIds: Int, totalIds: Int) -> Unit,
    ): ExternalIdLookupResult = withContext(Dispatchers.IO) {
        val uniqueIds = externalIds.filter { it > 0 }.distinct()
        if (uniqueIds.isEmpty()) {
            return@withContext ExternalIdLookupResult(resolvedByExternalId = emptyMap())
        }

        val requestedSet = uniqueIds.toHashSet()
        val result = linkedMapOf<Int, MutableList<ResolvedExternalAnime>>()
        val failedIds = linkedSetOf<Int>()
        var succeededBatches = 0
        var firstBatchError: Throwable? = null
        var processed = 0
        val chunks = uniqueIds.chunked(LOOKUP_BATCH_SIZE)

        chunks.forEachIndexed { index, chunk ->
            currentCoroutineContext().ensureActive()
            if (index > 0) delay(BATCH_LOOKUP_DELAY_MS)
            runSuspendCatching {
                fetchAllPages { limit, offset ->
                    when (format) {
                        ExternalListFormat.SHIKIMORI_JSON ->
                            api.lookupByShikimoriIds(ids = chunk, limit = limit, offset = offset)
                        ExternalListFormat.MAL_XML ->
                            api.lookupByMalIds(ids = chunk, limit = limit, offset = offset)
                    }
                }
            }.onSuccess { matchedDtos ->
                succeededBatches++
                matchedDtos.forEach { dto ->
                    val externalId = dto.matchedRequestedId(format, requestedSet) ?: return@forEach
                    val resolved = dto.toResolvedExternalAnime(externalId) ?: return@forEach
                    val bucket = result.getOrPut(externalId) { mutableListOf() }
                    if (bucket.none { it.animeId == resolved.animeId }) {
                        bucket += resolved
                    }
                }
            }.onFailure { error ->
                if (firstBatchError == null) firstBatchError = error
                failedIds += chunk
            }
            processed += chunk.size
            onBatchProgress(processed, uniqueIds.size)
        }

        if (succeededBatches == 0 && firstBatchError != null) {
            throw AnimeListTransferStageException(
                stage = AnimeListTransferStage.RESOLVE_EXTERNAL_IDS,
                cause = firstBatchError,
                completedBatches = 0,
                totalBatches = chunks.size,
            )
        }

        ExternalIdLookupResult(
            resolvedByExternalId = result,
            failedExternalIds = failedIds,
        )
    }

    private suspend fun fetchAllPages(
        fetchPage: suspend (limit: Int, offset: Int) -> List<YaniTransferLookupAnimeDto>,
    ): List<YaniTransferLookupAnimeDto> {
        val collected = mutableListOf<YaniTransferLookupAnimeDto>()
        var offset = 0
        repeat(MAX_PAGES_PER_BATCH) {
            val page = fetchWithRetry { fetchPage(LOOKUP_PAGE_LIMIT, offset) }
            collected += page
            if (page.size < LOOKUP_PAGE_LIMIT) return collected
            offset += LOOKUP_PAGE_LIMIT
            delay(BATCH_LOOKUP_DELAY_MS)
        }
        return collected
    }

    private suspend fun <T> fetchWithRetry(block: suspend () -> T): T {
        var lastError: Throwable? = null
        for (attempt in 0..MAX_LOOKUP_RETRIES) {
            val outcome = runSuspendCatching { block() }
            outcome.onSuccess { return it }
            val error = outcome.exceptionOrNull()!!
            lastError = error
            if (attempt >= MAX_LOOKUP_RETRIES || !error.isRetryableMutationFailure()) {
                throw error
            }
            delay(error.computeImportRetryDelayMs(attempt))
        }
        throw checkNotNull(lastError)
    }

    private companion object {
        /** Размер пачки меньше `LOOKUP_PAGE_LIMIT`, чтобы дубли в базе yani не срезали хвост. */
        const val LOOKUP_BATCH_SIZE = 50

        /** Максимальный `limit`, принимаемый `GET /anime` (`limit > 100` возвращает HTTP 400). */
        const val LOOKUP_PAGE_LIMIT = 100
        const val MAX_PAGES_PER_BATCH = 5
        const val BATCH_LOOKUP_DELAY_MS = 120L
        const val MAX_LOOKUP_RETRIES = 3

        const val MAX_LOCAL_IMPORT_EPISODES = 1_500
        const val SYNTHETIC_WATCHED_DURATION_MS = 24 * 60 * 1_000L

        const val SHARE_EXPORT_DIR = "list_exports"

        /** Общий `FileProvider` приложения (см. `AndroidManifest.xml` и `res/xml/log_file_paths.xml`). */
        const val SHARE_PROVIDER_SUFFIX = ".logs"
        const val IO_BUFFER_SIZE = 8 * 1024

        fun readBytesBounded(input: InputStream, maxBytes: Int): ByteArray {
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(IO_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > maxBytes) {
                    throw ExternalListParseException(ExternalListParseError.FILE_TOO_LARGE)
                }
                output.write(buffer, 0, read)
            }
            return output.toByteArray()
        }

        /**
         * Сопоставляет запись каталога строго по ID выбранного [format] без кросс-fallback:
         * иначе `shikimori_id` чужого тайтла мог бы совпасть с запрошенным `mal_id` и наоборот.
         */
        fun YaniTransferLookupAnimeDto.matchedRequestedId(
            format: ExternalListFormat,
            requestedIds: Set<Int>,
        ): Int? = externalIdFor(format)?.takeIf { it in requestedIds }
    }
}
