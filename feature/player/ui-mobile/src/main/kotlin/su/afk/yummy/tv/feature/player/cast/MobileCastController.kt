package su.afk.yummy.tv.feature.player.cast

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManager
import com.google.android.gms.cast.framework.SessionManagerListener
import com.google.android.gms.common.api.ResultCallback
import com.google.android.gms.common.images.WebImage as CastWebImage

internal data class MobileCastMedia(
    val url: String,
    val title: String,
    val subtitle: String,
    val imageUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val headers: Map<String, String>,
)

internal class MobileCastController(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val castContext: CastContext? = runCatching {
        CastContext.getSharedInstance(appContext)
    }.getOrNull()
    private val sessionManager: SessionManager? = castContext?.sessionManager

    var isCasting by mutableStateOf(false)
        private set
    var deviceName by mutableStateOf("")
        private set
    var lastRemotePositionMs by mutableLongStateOf(0L)
        private set

    private var pendingMedia: MobileCastMedia? = null
    private var pendingOnLoaded: (() -> Unit)? = null
    private var listenerRegistered = false

    private val sessionListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) = Unit

        override fun onSessionStarted(session: CastSession, sessionId: String) {
            updateSessionState(session)
            pendingMedia?.let { load(it, pendingOnLoaded ?: {}) }
        }

        override fun onSessionStartFailed(session: CastSession, error: Int) {
            isCasting = false
            deviceName = ""
        }

        override fun onSessionEnding(session: CastSession) {
            captureRemotePosition(session)
        }

        override fun onSessionEnded(session: CastSession, error: Int) {
            captureRemotePosition(session)
            isCasting = false
            deviceName = ""
        }

        override fun onSessionResuming(session: CastSession, sessionId: String) = Unit

        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) {
            updateSessionState(session)
            pendingMedia?.let { load(it, pendingOnLoaded ?: {}) }
        }

        override fun onSessionResumeFailed(session: CastSession, error: Int) {
            isCasting = false
            deviceName = ""
        }

        override fun onSessionSuspended(session: CastSession, reason: Int) {
            captureRemotePosition(session)
            isCasting = false
        }
    }

    fun start() {
        if (listenerRegistered) return
        sessionManager?.addSessionManagerListener(sessionListener, CastSession::class.java)
        listenerRegistered = true
        sessionManager?.currentCastSession?.takeIf { it.isConnected }?.let(::updateSessionState)
    }

    fun stop() {
        if (!listenerRegistered) return
        sessionManager?.removeSessionManagerListener(sessionListener, CastSession::class.java)
        listenerRegistered = false
    }

    fun load(media: MobileCastMedia, onLoaded: () -> Unit = {}): Boolean {
        pendingMedia = media
        pendingOnLoaded = onLoaded
        if (media.headers.isNotEmpty()) return false
        val session = sessionManager?.currentCastSession?.takeIf { it.isConnected }
            ?: return false
        val remoteMediaClient = session.remoteMediaClient ?: return false
        val request = MediaLoadRequestData.Builder()
            .setMediaInfo(media.toMediaInfo())
            .setAutoplay(true)
            .setCurrentTime(media.positionMs.coerceAtLeast(0L))
            .build()

        remoteMediaClient.load(request).setResultCallback(ResultCallback { result ->
            if (result.status.isSuccess) {
                updateSessionState(session)
                onLoaded()
            }
        })
        return true
    }

    fun stopCasting() {
        captureRemotePosition(sessionManager?.currentCastSession)
        sessionManager?.endCurrentSession(true)
    }

    fun remotePositionMs(): Long {
        captureRemotePosition(sessionManager?.currentCastSession)
        return lastRemotePositionMs
    }

    private fun updateSessionState(session: CastSession) {
        isCasting = session.isConnected
        deviceName = session.castDevice?.friendlyName.orEmpty()
    }

    private fun captureRemotePosition(session: CastSession?) {
        val position = session?.remoteMediaClient?.approximateStreamPosition ?: return
        lastRemotePositionMs = position.coerceAtLeast(0L)
    }

    private fun MobileCastMedia.toMediaInfo(): MediaInfo {
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, title)
            if (subtitle.isNotBlank()) putString(MediaMetadata.KEY_SUBTITLE, subtitle)
            imageUrl.takeIf { it.isNotBlank() }?.let { addImage(CastWebImage(Uri.parse(it))) }
        }
        return MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(contentTypeFor(url))
            .setMetadata(metadata)
            .apply { if (durationMs > 0L) setStreamDuration(durationMs) }
            .build()
    }

    private fun contentTypeFor(url: String): String {
        val cleanUrl = url.substringBefore('?').substringBefore('#')
        return when {
            cleanUrl.endsWith(".m3u8", ignoreCase = true) -> "application/x-mpegURL"
            cleanUrl.endsWith(".mpd", ignoreCase = true) -> "application/dash+xml"
            cleanUrl.endsWith(".mp4", ignoreCase = true) -> "video/mp4"
            else -> "video/mp4"
        }
    }
}

@Composable
internal fun rememberMobileCastController(): MobileCastController {
    val context = LocalContext.current
    val controller = remember(context) { MobileCastController(context) }
    DisposableEffect(controller) {
        controller.start()
        onDispose { controller.stop() }
    }
    return controller
}
