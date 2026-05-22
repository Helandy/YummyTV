package su.afk.yummy.tv.feature.player.extractor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.stream.StreamInfo
import su.afk.yummy.tv.feature.player.view.youtube.NewPipeDownloader

private val youtubeIdRegex = Regex("""youtube\.com/embed/([a-zA-Z0-9_-]+)""")

object YouTubeExtractor {

    @Volatile private var initialized = false

    private fun ensureInit() {
        if (!initialized) synchronized(this) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloader.Companion.instance)
                initialized = true
            }
        }
    }

    suspend fun extract(iframeUrl: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            ensureInit()
            val videoId = youtubeIdRegex.find(iframeUrl)?.groupValues?.get(1)
                ?: return@withContext null
            val info = StreamInfo.getInfo(
                NewPipe.getService(0),
                "https://www.youtube.com/watch?v=$videoId",
            )
            info.videoStreams.maxByOrNull { it.height }?.content
        }.getOrNull()
    }
}
