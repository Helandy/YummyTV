package su.afk.yummy.tv.feature.player.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import su.afk.yummy.tv.feature.player.PlayerState
import su.afk.yummy.tv.feature.player.utils.QualityAdaptationDecision
import su.afk.yummy.tv.feature.player.utils.QualityAdaptationPolicy

/**
 * Режим «Адаптивное» качество для ТВ и мобилки: понижает качество, когда устройство или сеть не
 * тянут текущее, и возвращает обратно при улучшении. Решения принимает [QualityAdaptationPolicy],
 * здесь только сбор сигналов плеера и отправка [PlayerState.Event.QualitySelected] с `auto = true`.
 *
 * Политика пересоздаётся при смене набора качеств (новая серия или источник), поэтому память о
 * проблемных уровнях живёт в пределах одного потока. Остановки после перемотки, пропуска опенинга и
 * замены item (смена сабов) не считаются.
 */
@Composable
fun PlayerQualityAdaptationEffect(
    player: Player,
    enabled: Boolean,
    qualities: List<String>,
    activeQuality: String?,
    ceilingHeight: Int,
    onEvent: (PlayerState.Event) -> Unit,
) {
    if (!enabled || qualities.size < 2) return
    val signals = rememberPlayerQualitySignals()
    val currentOnEvent by rememberUpdatedState(onEvent)
    val currentQuality by rememberUpdatedState(activeQuality)
    val currentQualities by rememberUpdatedState(qualities)
    val policy = remember(qualities, ceilingHeight) { QualityAdaptationPolicy(ceilingHeight) }

    // Любая смена качества (наша или ручная) обнуляет накопленные факты и открывает cooldown.
    LaunchedEffect(policy, activeQuality) { policy.onQualityChanged(nowMs()) }

    DisposableEffect(player, policy) {
        var wasReady = false
        var hasPlayed = false
        var seekPending = false
        val listener = object : Player.Listener {
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                // Любой управляемый разрыв (перемотка, пропуск опенинга, замена item при смене
                // сабов/озвучки) даёт буферизацию, не связанную с сетью. Идущая остановка не в счёт.
                if (reason != Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                    seekPending = true
                    policy.onStallEnded()
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) {
                    seekPending = true
                    policy.onStallEnded()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        policy.onStallEnded()
                        wasReady = true
                        hasPlayed = true
                        seekPending = false
                    }

                    Player.STATE_BUFFERING -> {
                        // Перемотка на возобновлении тоже шлёт SEEK, но до первого кадра это старт.
                        val countable = !hasPlayed || (wasReady && !seekPending)
                        if (countable && player.playWhenReady) {
                            policy.onStallStarted(nowMs(), startup = !hasPlayed)
                        }
                        wasReady = false
                    }

                    else -> {
                        wasReady = false
                        policy.onStallEnded()
                    }
                }
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    LaunchedEffect(player, policy) {
        signals.events.collect { signal ->
            when (signal) {
                is PlayerQualitySignals.Signal.DroppedFrames ->
                    policy.onDroppedFrames(signal.count, nowMs())

                is PlayerQualitySignals.Signal.Bandwidth ->
                    policy.onBandwidth(signal.bitsPerSecond)
            }
        }
    }

    LaunchedEffect(player, policy) {
        while (true) {
            delay(EVALUATE_INTERVAL_MS)
            val bufferedAheadMs = (player.bufferedPosition - player.currentPosition).coerceAtLeast(0L)
            val decision = policy.evaluate(currentQuality, currentQualities, bufferedAheadMs, nowMs())
            if (decision is QualityAdaptationDecision.Switch) {
                currentOnEvent(
                    PlayerState.Event.QualitySelected(
                        quality = decision.label,
                        currentPosMs = player.currentPosition.coerceAtLeast(0L),
                        auto = true,
                    ),
                )
            }
        }
    }
}

private fun nowMs(): Long = android.os.SystemClock.elapsedRealtime()

private const val EVALUATE_INTERVAL_MS = 5_000L
