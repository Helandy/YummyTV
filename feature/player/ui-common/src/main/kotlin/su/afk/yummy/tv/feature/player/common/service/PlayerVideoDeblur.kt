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
 * Рендерер Media3 поднимает GL-конвейер (VideoSink), если до включения рендерера получил список эффектов — даже
 * пустой, — и освобождает его только вместе с плеером. GL-путь идёт мимо аппаратного вывода и режима картинки ТВ,
 * поэтому без Deblur конвейера быть не должно:
 * - при выключенном Deblur и плеере без конвейера `setVideoEffects` не вызывается вовсе;
 * - включение на лету перезапускает поток (stop + prepare с той же позиции): рендерер получает эффекты до
 *   включения и поднимает конвейер;
 * - выключение после включения просит сервис пересобрать плеер ([rebuildPlayer]); новый плеер собирается без
 *   конвейера и передаётся сюда через [attach].
 */
@OptIn(UnstableApi::class)
internal class PlayerVideoDeblur(
    private var player: ExoPlayer,
    private var pipelineReady: Boolean,
    private val effects: List<Effect>,
    private val rebuildPlayer: () -> Unit,
) {
    fun start(scope: CoroutineScope, enabledFlow: Flow<Boolean>) {
        enabledFlow
            .distinctUntilChanged()
            .onEach(::apply)
            .launchIn(scope)
    }

    /** Переключает на пересобранный плеер; [pipelineReady] — собран ли он с конвейером эффектов. */
    fun attach(player: ExoPlayer, pipelineReady: Boolean) {
        this.player = player
        this.pipelineReady = pipelineReady
    }

    private fun apply(enabled: Boolean) {
        when {
            enabled && pipelineReady -> player.setVideoEffects(effects)
            enabled -> {
                player.setVideoEffects(effects)
                pipelineReady = true
                restartStream()
            }
            pipelineReady -> rebuildPlayer()
        }
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
