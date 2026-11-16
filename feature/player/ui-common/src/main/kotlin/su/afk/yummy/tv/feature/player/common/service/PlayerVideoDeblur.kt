package su.afk.yummy.tv.feature.player.common.service

import androidx.annotation.OptIn
import androidx.media3.common.Effect
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Применяет видеошейдер Deblur по настройке во время воспроизведения.
 *
 * Конвейер эффектов Media3 создаётся только при включении видеорендерера, если эффекты заданы заранее. Если
 * плеер собран с Deblur ([pipelineReady]), эффекты меняются на лету. Если нет — при включении поток
 * перезапускается (stop + prepare с той же позиции): так рендерер пересоздаётся уже с конвейером.
 */
@OptIn(UnstableApi::class)
internal class PlayerVideoDeblur(
    private val player: ExoPlayer,
    private var pipelineReady: Boolean,
    private val effects: List<Effect>,
) {
    fun start(scope: CoroutineScope, enabledFlow: Flow<Boolean>) {
        enabledFlow
            .distinctUntilChanged()
            .onEach(::apply)
            .launchIn(scope)
    }

    private fun apply(enabled: Boolean) {
        player.setVideoEffects(if (enabled) effects else emptyList())
        if (!enabled || pipelineReady) return
        pipelineReady = true
        restartStream()
    }

    private fun restartStream() {
        if (player.playbackState == Player.STATE_IDLE || player.currentMediaItem == null) return
        val position = player.currentPosition
        val playWhenReady = player.playWhenReady
        player.stop()
        player.seekTo(position)
        player.prepare()
        player.playWhenReady = playWhenReady
    }
}
