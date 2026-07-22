package su.afk.yummy.tv.data.videodownload.worker

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.DocumentsContract
import androidx.annotation.OptIn
import androidx.hilt.work.HiltWorker
import androidx.media3.common.MediaItem
import androidx.media3.common.util.Clock
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSourceBitmapLoader
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultAssetLoaderFactory
import androidx.media3.transformer.DefaultDecoderFactory
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import su.afk.yummy.tv.data.videodownload.R
import su.afk.yummy.tv.data.videodownload.cache.VideoDownloadCacheProvider
import su.afk.yummy.tv.data.videodownload.notification.VideoExportNotificationService
import su.afk.yummy.tv.data.videodownload.strategy.DownloadPlayerStrategyResolver
import su.afk.yummy.tv.data.videodownload.utils.treeDocumentUri
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import java.io.File
import kotlin.math.roundToInt
import kotlin.Result as KotlinResult

@OptIn(UnstableApi::class)
@HiltWorker
class VideoExportWorker @AssistedInject internal constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val downloadRepository: VideoDownloadRepository,
    private val exportRepository: VideoDownloadExportRepository,
    private val cacheProvider: VideoDownloadCacheProvider,
    private val strategyResolver: DownloadPlayerStrategyResolver,
    private val notificationService: VideoExportNotificationService,
    private val analytics: VideoExportAnalytics,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val downloadId = inputData.getLong(KEY_DOWNLOAD_ID, 0L).takeIf { it > 0L }
            ?: return Result.failure()
        val destinationUri = inputData.getString(KEY_DESTINATION_URI).orEmpty()
        val item = downloadRepository.getDownload(downloadId)
            ?: return Result.failure()
        if (item.status != VideoDownloadStatus.Downloaded || destinationUri.isBlank()) {
            return Result.failure()
        }
        setForeground(notificationService.createForegroundInfo(item, 0))
        return try {
            exportMutex.withLock { export(downloadId, destinationUri, item) }
        } finally {
            notificationService.cancel(downloadId)
        }
    }

    private suspend fun export(
        downloadId: Long,
        destinationUri: String,
        item: VideoDownloadItem,
    ): Result {
        val tempDirectory = File(applicationContext.cacheDir, TEMP_DIRECTORY_NAME)
        val tempFile = File(tempDirectory, "$downloadId.mp4")
        var createdDocumentUri: Uri? = null
        val startedAtMs = System.currentTimeMillis()
        return try {
            // Проверяем доступ до перекодирования: иначе пользователь ждёт минуты ради SecurityException
            check(exportRepository.isDestinationWritable(destinationUri)) {
                applicationContext.getString(R.string.video_export_error_directory_unavailable)
            }
            ensureTemporarySpace(item)
            tempDirectory.mkdirs()
            tempFile.delete()
            exportRepository.updateState(
                downloadId = downloadId,
                status = VideoExportStatus.Preparing,
                progress = 0f,
                destinationUri = destinationUri,
            )
            transformCachedMedia(item, tempFile)
            createdDocumentUri = createExportDocument(
                treeUri = Uri.parse(destinationUri),
                item = item,
            )
            val exportedBytes = tempFile.length()
            copyToDocument(item, tempFile, createdDocumentUri)
            analytics.reportSucceeded(
                item = item,
                durationMs = System.currentTimeMillis() - startedAtMs,
                bytes = exportedBytes,
            )
            exportRepository.updateState(
                downloadId = downloadId,
                status = VideoExportStatus.Exported,
                progress = 1f,
                destinationUri = destinationUri,
                exportedFileUri = createdDocumentUri.toString(),
            )
            Result.success()
        } catch (throwable: Throwable) {
            createdDocumentUri?.let { uri ->
                runCatching {
                    DocumentsContract.deleteDocument(
                        applicationContext.contentResolver,
                        uri
                    )
                }
            }
            if (throwable is CancellationException) throw throwable
            analytics.reportFailed(
                item = item,
                details = throwable.userFacingExportError(),
                throwable = throwable,
            )
            exportRepository.updateState(
                downloadId = downloadId,
                status = VideoExportStatus.Failed,
                destinationUri = destinationUri,
                errorMessage = throwable.userFacingExportError(),
            )
            Result.success()
        } finally {
            tempFile.delete()
        }
    }

    private fun ensureTemporarySpace(item: VideoDownloadItem) {
        val available = StatFs(applicationContext.cacheDir.path).availableBytes
        val required = item.bytesDownloaded.coerceAtLeast(MINIMUM_ESTIMATED_EXPORT_BYTES) +
                TEMP_SPACE_RESERVE_BYTES
        check(available >= required) {
            applicationContext.getString(R.string.video_export_error_no_space)
        }
    }

    private suspend fun transformCachedMedia(item: VideoDownloadItem, output: File) {
        val strategy = strategyResolver.resolve(item)
        val streamKind = item.streamUrl.streamKind()
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(cacheProvider.cache)
            .setFlags(CacheDataSource.FLAG_BLOCK_ON_CACHE)
            .apply {
                strategy.cacheKeyFactory(item.cacheKey, item.streamUrl, streamKind)
                    ?.let(::setCacheKeyFactory)
            }
        val mediaSourceFactory = DefaultMediaSourceFactory(applicationContext)
            .setDataSourceFactory(cacheDataSourceFactory)
        val bitmapLoader = DataSourceBitmapLoader.Builder(applicationContext).build()
        val assetLoaderFactory = DefaultAssetLoaderFactory(
            applicationContext,
            DefaultDecoderFactory.Builder(applicationContext).build(),
            Clock.DEFAULT,
            mediaSourceFactory,
            bitmapLoader,
        )
        val completion = CompletableDeferred<KotlinResult<Unit>>()
        val transformer = withContext(Dispatchers.Main.immediate) {
            Transformer.Builder(applicationContext)
                .setAssetLoaderFactory(assetLoaderFactory)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult,
                        ) {
                            completion.complete(KotlinResult.success(Unit))
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            completion.complete(KotlinResult.failure(exportException))
                        }
                    }
                )
                .build()
                .also { transformer ->
                    transformer.start(
                        MediaItem.Builder()
                            .setUri(item.streamUrl)
                            .setCustomCacheKey(item.cacheKey)
                            .build(),
                        output.absolutePath,
                    )
                }
        }
        val progressHolder = ProgressHolder()
        try {
            while (!completion.isCompleted) {
                val transformProgress = withContext(Dispatchers.Main.immediate) {
                    if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                        progressHolder.progress.coerceIn(0, 100)
                    } else {
                        0
                    }
                }
                updateProgress(item, transformProgress * TRANSFORM_PROGRESS_SHARE / 100f)
                delay(PROGRESS_POLL_INTERVAL_MS)
            }
            completion.await().getOrThrow()
        } finally {
            if (!completion.isCompleted) {
                withContext(Dispatchers.Main.immediate) { transformer.cancel() }
            }
        }
    }

    /** Каждый тайтл получает свою подпапку, внутри — файл вида «Серия N_Озвучка_Качество_Балансер». */
    private fun createExportDocument(treeUri: Uri, item: VideoDownloadItem): Uri {
        val resolver = applicationContext.contentResolver
        val titleDirectoryUri = findOrCreateDirectory(
            treeUri = treeUri,
            parentDocumentUri = treeUri.treeDocumentUri(),
            name = item.exportDirectoryName(),
        )
        val requestedName = item.exportFileName()
        // Имя детерминированное, поэтому файл с таким же именем — это та же серия: заменяем его
        readChildNames(treeUri, titleDirectoryUri)
            .firstOrNull { it.name == requestedName }
            ?.let { existing ->
                runCatching {
                    DocumentsContract.deleteDocument(
                        resolver,
                        DocumentsContract.buildDocumentUriUsingTree(treeUri, existing.documentId),
                    )
                }
            }
        return checkNotNull(
            DocumentsContract.createDocument(
                resolver,
                titleDirectoryUri,
                MP4_MIME_TYPE,
                requestedName,
            )
        ) { applicationContext.getString(R.string.video_export_error_create_file) }
    }

    private fun findOrCreateDirectory(treeUri: Uri, parentDocumentUri: Uri, name: String): Uri {
        val existing = readChildNames(treeUri, parentDocumentUri)
            .firstOrNull { it.name == name && it.mimeType == DocumentsContract.Document.MIME_TYPE_DIR }
        if (existing != null) {
            return DocumentsContract.buildDocumentUriUsingTree(treeUri, existing.documentId)
        }
        return checkNotNull(
            DocumentsContract.createDocument(
                applicationContext.contentResolver,
                parentDocumentUri,
                DocumentsContract.Document.MIME_TYPE_DIR,
                name,
            )
        ) { applicationContext.getString(R.string.video_export_error_create_directory) }
    }

    private fun readChildNames(treeUri: Uri, parentDocumentUri: Uri): List<DocumentChild> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getDocumentId(parentDocumentUri),
        )
        return applicationContext.contentResolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex =
                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        DocumentChild(
                            documentId = cursor.getString(idIndex).orEmpty(),
                            name = cursor.getString(nameIndex).orEmpty(),
                            mimeType = cursor.getString(mimeIndex).orEmpty(),
                        )
                    )
                }
            }
        }.orEmpty()
    }

    private data class DocumentChild(
        val documentId: String,
        val name: String,
        val mimeType: String,
    )

    private suspend fun copyToDocument(item: VideoDownloadItem, source: File, destination: Uri) =
        withContext(Dispatchers.IO) {
            exportRepository.updateState(
                downloadId = item.id,
                status = VideoExportStatus.Copying,
                progress = TRANSFORM_PROGRESS_SHARE,
            )
            applicationContext.contentResolver.openOutputStream(destination, "w").use { output ->
                checkNotNull(output) {
                    applicationContext.getString(R.string.video_export_error_open_file)
                }
                source.inputStream().buffered().use { input ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    var copied = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        copied += read
                        val copyFraction = if (source.length() > 0L) {
                            copied.toFloat() / source.length().toFloat()
                        } else 0f
                        updateProgress(
                            item,
                            TRANSFORM_PROGRESS_SHARE +
                                    copyFraction.coerceIn(0f, 1f) * COPY_PROGRESS_SHARE,
                            VideoExportStatus.Copying,
                        )
                    }
                    output.flush()
                }
            }
        }

    private suspend fun updateProgress(
        item: VideoDownloadItem,
        progress: Float,
        status: VideoExportStatus = VideoExportStatus.Preparing,
    ) {
        exportRepository.updateState(
            downloadId = item.id,
            status = status,
            progress = progress,
        )
        setForeground(
            notificationService.createForegroundInfo(
                item,
                (progress.coerceIn(0f, 1f) * 100).roundToInt(),
            )
        )
    }

    private fun VideoDownloadItem.exportDirectoryName(): String =
        animeTitle.toSafeName().ifBlank { DEFAULT_DIRECTORY_NAME }

    private fun VideoDownloadItem.exportFileName(): String {
        val balancer = playerName.balancerLabel()
        val safe = listOf(
            "Серия $episode",
            dubbing.ifBlank { balancer },
            qualityLabel,
            balancer,
        )
            .map { it.toSafeName() }
            .filter { it.isNotBlank() }
            .joinToString("_")
            .take(MAX_FILE_NAME_BASE_LENGTH)
            .trim(' ', '.', '_')
            .ifBlank { "Серия $episode".toSafeName().ifBlank { DEFAULT_DIRECTORY_NAME } }
        return "$safe$MP4_EXTENSION"
    }

    private fun String.balancerLabel(): String =
        trim().removePrefix("Плеер ").removePrefix("Player ")

    private fun String.toSafeName(): String =
        replace(FORBIDDEN_FILE_NAME_CHARS, " ")
            .replace(CONTROL_CHARS, " ")
            .replace(WHITESPACE, " ")
            .trim(' ', '.')
            .take(MAX_FILE_NAME_BASE_LENGTH)
            .trim()

    private fun Throwable.userFacingExportError(): String {
        val message = message?.takeIf(String::isNotBlank).orEmpty()
        return if (message.isNotBlank()) message else
            applicationContext.getString(R.string.video_export_error_unknown)
    }

    companion object {
        const val KEY_DOWNLOAD_ID = "download_id"
        const val KEY_DESTINATION_URI = "destination_uri"

        private val exportMutex = Mutex()
        private val FORBIDDEN_FILE_NAME_CHARS = Regex("""[/\\:*?\"<>|]""")
        private val CONTROL_CHARS = Regex("[\\u0000-\\u001F\\u007F]")
        private val WHITESPACE = Regex("\\s+")
        private const val TEMP_DIRECTORY_NAME = "video_exports"
        private const val MP4_EXTENSION = ".mp4"
        private const val MP4_MIME_TYPE = "video/mp4"
        private const val MAX_FILE_NAME_BASE_LENGTH = 180
        private const val DEFAULT_DIRECTORY_NAME = "YummyTV"
        private const val COPY_BUFFER_BYTES = 256 * 1024
        private const val PROGRESS_POLL_INTERVAL_MS = 500L
        private const val TRANSFORM_PROGRESS_SHARE = 0.85f
        private const val COPY_PROGRESS_SHARE = 0.15f
        private const val MINIMUM_ESTIMATED_EXPORT_BYTES = 32L * 1024 * 1024
        private const val TEMP_SPACE_RESERVE_BYTES = 128L * 1024 * 1024
    }
}
