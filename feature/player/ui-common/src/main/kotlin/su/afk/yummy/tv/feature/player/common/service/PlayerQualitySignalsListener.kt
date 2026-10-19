package su.afk.yummy.tv.feature.player.common.service

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import su.afk.yummy.tv.feature.player.common.PlayerQualitySignals

/** Передаёт в [PlayerQualitySignals] пропущенные кадры и оценку полосы для адаптивного качества. */
@OptIn(UnstableApi::class)
internal class PlayerQualitySignalsListener(
    private val signals: PlayerQualitySignals,
) : AnalyticsListener {

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long,
    ) {
        signals.emitDroppedFrames(droppedFrames)
    }

    override fun onBandwidthEstimate(
        eventTime: AnalyticsListener.EventTime,
        totalLoadTimeMs: Int,
        totalBytesLoaded: Long,
        bitrateEstimate: Long,
    ) {
        signals.emitBandwidth(bitrateEstimate)
    }
}
