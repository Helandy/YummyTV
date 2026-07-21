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
import su.afk.yummy.tv.data.videodownload.worker.utils.streamKind
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadExportRepository
import su.afk.yummy.tv.domain.videodownload.repository.VideoDownloadRepository
import java.io.File
import kotlin.math.roundToInt

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
        return try {
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
            createdDocumentUri = createUniqueDocument(
                treeUri = Uri.parse(destinationUri),
                requestedName = item.exportFileName(),
            )
            copyToDocument(item, tempFile, createdDocumentUri)
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
        val completion = CompletableDeferred<Result<Unit>>()
        val transformer = withContext(Dispatchers.Main.immediate) {
            Transformer.Builder(applicationContext)
                .setAssetLoaderFactory(assetLoaderFactory)
                .addListener(
                    object : Transformer.Listener {
                        override fun onCompleted(
                            composition: Composition,
                            exportResult: ExportResult,
                        ) {
                            completion.complete(Result.success(Unit))
                        }

                        override fun onError(
                            composition: Composition,
                            exportResult: ExportResult,
                            exportException: ExportException,
                        ) {
                            completion.complete(Result.failure(exportException))
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

    private fun createUniqueDocument(treeUri: Uri, requestedName: String): Uri {
        val resolver = applicationContext.contentResolver
        val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val existingNames = mutableSetOf<String>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        resolver.query(
            childrenUri,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            while (cursor.moveToNext()) existingNames += cursor.getString(0)
        }
        val baseName = requestedName.removeSuffix(MP4_EXTENSION)
        var candidate = requestedName
        var suffix = 2
        while (candidate in existingNames) {
            candidate = "$baseName ($suffix)$MP4_EXTENSION"
            suffix += 1
        }
        return checkNotNull(
            DocumentsContract.createDocument(
                resolver,
                parentDocumentUri,
                MP4_MIME_TYPE,
                candidate,
            )
        ) { applicationContext.getString(R.string.video_export_error_create_file) }
    }

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

    private fun VideoDownloadItem.exportFileName(): String {
        val raw = "$animeTitle — серия $episode — ${dubbing.ifBlank { playerName }} — $qualityLabel"
        val safe = raw
            .replace(FORBIDDEN_FILE_NAME_CHARS, " ")
            .replace(CONTROL_CHARS, " ")
            .replace(WHITESPACE, " ")
            .trim(' ', '.')
            .take(MAX_FILE_NAME_BASE_LENGTH)
            .trim()
            .ifBlank { "YummyTV — серия $episode" }
        return "$safe$MP4_EXTENSION"
    }

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
        private const val COPY_BUFFER_BYTES = 256 * 1024
        private const val PROGRESS_POLL_INTERVAL_MS = 500L
        private const val TRANSFORM_PROGRESS_SHARE = 0.85f
        private const val COPY_PROGRESS_SHARE = 0.15f
        private const val MINIMUM_ESTIMATED_EXPORT_BYTES = 32L * 1024 * 1024
        private const val TEMP_SPACE_RESERVE_BYTES = 128L * 1024 * 1024
    }
}
