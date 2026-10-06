package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.core.utils.player.isOkCdnUrl
import su.afk.yummy.tv.core.utils.player.withCdnHost
import su.afk.yummy.tv.feature.player.PlayerState

/*
 * Переезд потока на резервный узел CDN без перерезолва источника.
 * Механика целиком, вместе с замерами отказов узлов — в `docs/cvh-player.md`.
 */

/**
 * Узел CDN, на который можно переехать прямо сейчас, или null — переезд недоступен либо уже сделан.
 *
 * Проверяется фактически проигрываемая ссылка, а не [PlayerState.State.streamUrl]: качество могли
 * переключить, и тогда играет другой URL из карты качеств. Признаком израсходованного переезда
 * служит сам хост — после подмены он равен резервному, поэтому второй раз условие не выполнится и
 * отдельный флаг не нужен.
 */
internal fun PlayerState.State.availableFailoverHost(): String? {
    val failoverHost = streamFailoverHost?.takeIf { it.isNotBlank() } ?: return null
    val playbackUrl = activeStreamUrl()?.takeIf { it.isOkCdnUrl() } ?: return null
    return failoverHost.takeIf { it != playbackUrl.streamHost() }
}

/**
 * Переводит поток и все качества на [host]: подпись okcdn от узла не зависит, поэтому те же ссылки
 * на резервном узле отдают тот же файл.
 *
 * Подменяется вся карта качеств, а не только текущая ссылка, иначе переключение качества после
 * переезда вернуло бы на отказавший узел. Последний кадр остаётся на экране
 * ([PlayerState.State.isPlaybackRecovering]), поэтому пауза в воспроизведении не видна.
 */
internal fun PlayerState.State.withFailoverHost(host: String): PlayerState.State = copy(
    streamUrl = streamUrl?.withCdnHost(host),
    streamQualityMap = streamQualityMap
        ?.mapValuesTo(LinkedHashMap()) { (_, url) -> url.withCdnHost(host) },
    playerError = null,
    isPlaybackRecovering = true,
    showChangePlayerHint = false,
    // Фатальная ошибка могла обнулить позицию, а media item берёт её как точку возобновления.
    resumeFromMs = maxOf(resumeFromMs, playbackPositionMs),
)

/** Ссылка, которая играет сейчас: с учётом выбранного качества. */
private fun PlayerState.State.activeStreamUrl(): String? =
    activeQuality(this)?.let { quality -> streamQualities(this)[quality] } ?: streamUrl
