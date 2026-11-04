package su.afk.yummy.tv.domain.player.model

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail

data class PlayerStreamRequest(
    val iframeUrl: String,
    val autoQualityLabel: String,
    val sessionFallbackTtlSeconds: Int? = null,
    val reusePlaybackSession: Boolean = true,
    /** Пропускает и перезаписывает закэшированный resolve — предыдущий результат известно плохой. */
    val forceRefresh: Boolean = false,
    /** Сообщает о текущем шаге получения потока; вызывается из потока резолвера. */
    val onStatus: (PlayerStreamResolveStatus) -> Unit = {},
    /** Подробности сетевых запросов резолва (хост, код ответа, время); вызывается из потока резолвера. */
    val onDetail: (ResolveDetail) -> Unit = {},
)
