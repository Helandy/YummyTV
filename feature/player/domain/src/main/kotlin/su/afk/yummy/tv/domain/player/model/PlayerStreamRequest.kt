package su.afk.yummy.tv.domain.player.model

import su.afk.yummy.tv.core.common.player.PlayerStreamResolveStatus
import su.afk.yummy.tv.core.common.player.ResolveDetail

data class PlayerStreamRequest(
    val iframeUrl: String,
    /** Ссылка видео, которое плеер разрешал перед этим запросом; `null`, если это первое видео экрана. */
    val previousIframeUrl: String? = null,
    val autoQualityLabel: String,
    /** Пропускает и перезаписывает закэшированный resolve — предыдущий результат известно плохой. */
    val forceRefresh: Boolean = false,
    /** Сообщает о текущем шаге получения потока; вызывается из потока резолвера. */
    val onStatus: (PlayerStreamResolveStatus) -> Unit = {},
    /** Подробности сетевых запросов резолва (хост, код ответа, время); вызывается из потока резолвера. */
    val onDetail: (ResolveDetail) -> Unit = {},
    /** Текущая позиция воспроизведения, мс; источник, следящий за просмотром (Alloha), сверяет с ней сессию. */
    val positionMs: () -> Long = { 0L },
    /** Длительность текущего видео, мс; 0, пока неизвестна. */
    val durationMs: () -> Long = { 0L },
)
