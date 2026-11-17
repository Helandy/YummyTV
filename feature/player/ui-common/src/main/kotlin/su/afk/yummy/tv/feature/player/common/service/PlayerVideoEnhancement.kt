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
import su.afk.yummy.tv.core.model.settings.VideoEnhancementMode

/**
 * Применяет режим улучшения видео (резкость или Anime4K) во время воспроизведения.
 *
 * Рендерер Media3 поднимает GL-конвейер (VideoSink), если до включения рендерера получил список эффектов — даже
 * пустой, — и освобождает его только вместе с плеером. GL-путь идёт мимо аппаратного вывода и режима картинки ТВ,
 * поэтому без улучшения конвейера быть не должно:
 * - режим «выключено» на плеере без конвейера `setVideoEffects` не вызывает вовсе;
 * - включение на лету перезапускает поток (stop + prepare с той же позиции): рендерер получает эффекты до
 *   включения и поднимает конвейер;
 * - смена одного режима на другой при готовом конвейере только подменяет эффекты;
 * - выключение после включения просит сервис пересобрать плеер ([rebuildPlayer]); новый плеер собирается без
 *   конвейера и передаётся сюда через [attach].
 */
@OptIn(UnstableApi::class)
internal class PlayerVideoEnhancement(
    private var player: ExoPlayer,
    private var pipelineReady: Boolean,
    private val effectsFor: (VideoEnhancementMode) -> List<Effect>,
    private val rebuildPlayer: () -> Unit,
) {
    /** Режим, применённый к текущему плееру. */
    var mode: VideoEnhancementMode = VideoEnhancementMode.OFF
        private set

    fun start(scope: CoroutineScope, modeFlow: Flow<VideoEnhancementMode>) {
        modeFlow
            .distinctUntilChanged()
            .onEach(::apply)
            .launchIn(scope)
    }

    /** Переключает на пересобранный плеер; [pipelineReady] — собран ли он с конвейером эффектов. */
    fun attach(player: ExoPlayer, pipelineReady: Boolean) {
        this.player = player
        this.pipelineReady = pipelineReady
    }

    private fun apply(mode: VideoEnhancementMode) {
        this.mode = mode
        when {
            mode == VideoEnhancementMode.OFF -> if (pipelineReady) rebuildPlayer()
            pipelineReady -> player.setVideoEffects(effectsFor(mode))
            else -> {
                player.setVideoEffects(effectsFor(mode))
                pipelineReady = true
                restartStream()
            }
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
