package su.afk.yummy.tv.feature.player.common.service

import android.view.Surface
import android.view.SurfaceHolder
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi

/**
 * Плеер медиа-сессии, запоминающий поверхность вывода, которую прислал контроллер экрана.
 *
 * `MediaSession.setPlayer` не переносит поверхность на новый плеер, а контроллер повторно её не шлёт.
 * Поэтому при подмене плеера (см. [PlayerMediaSessionService]) поверхность забирается у старого
 * через [detachVideoOutput] и отдаётся новому через [attachVideoOutput].
 */
@OptIn(UnstableApi::class)
internal class PlayerVideoOutputTracker(player: Player) : ForwardingPlayer(player) {
    private var surface: Surface? = null
    private var surfaceHolder: SurfaceHolder? = null

    override fun setVideoSurface(surface: Surface?) {
        this.surface = surface
        surfaceHolder = null
        super.setVideoSurface(surface)
    }

    override fun clearVideoSurface() {
        surface = null
        surfaceHolder = null
        super.clearVideoSurface()
    }

    override fun clearVideoSurface(surface: Surface?) {
        if (surface != null && surface === this.surface) this.surface = null
        super.clearVideoSurface(surface)
    }

    override fun setVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        this.surfaceHolder = surfaceHolder
        surface = null
        super.setVideoSurfaceHolder(surfaceHolder)
    }

    override fun clearVideoSurfaceHolder(surfaceHolder: SurfaceHolder?) {
        if (surfaceHolder != null && surfaceHolder === this.surfaceHolder) this.surfaceHolder = null
        super.clearVideoSurfaceHolder(surfaceHolder)
    }

    /**
     * Отцепляет поверхность от текущего плеера и возвращает её для [attachVideoOutput] нового.
     * Поверхность принимает только одного производителя кадров: новый плеер подключают уже после
     * release() старого.
     */
    fun detachVideoOutput(): VideoOutput? {
        val holder = surfaceHolder
        val plainSurface = surface
        surfaceHolder = null
        surface = null
        return when {
            holder != null -> VideoOutput.Holder(holder).also { wrappedPlayer.clearVideoSurfaceHolder(holder) }
            plainSurface != null -> VideoOutput.Plain(plainSurface).also { wrappedPlayer.clearVideoSurface(plainSurface) }
            else -> null
        }
    }

    fun attachVideoOutput(output: VideoOutput?) {
        when (output) {
            is VideoOutput.Holder -> setVideoSurfaceHolder(output.holder)
            is VideoOutput.Plain -> setVideoSurface(output.surface)
            null -> Unit
        }
    }

    /** Поверхность вывода в том виде, в каком её передала сессия. */
    sealed interface VideoOutput {
        class Holder(val holder: SurfaceHolder) : VideoOutput
        class Plain(val surface: Surface) : VideoOutput
    }
}
