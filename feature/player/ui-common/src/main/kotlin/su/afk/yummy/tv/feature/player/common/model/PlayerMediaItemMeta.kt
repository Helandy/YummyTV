package su.afk.yummy.tv.feature.player.common.model

import androidx.compose.runtime.Immutable

/** Тексты медиа-сессии (уведомление, «Сейчас играет» на ТВ) для текущей серии. */
@Immutable
data class PlayerMediaItemMeta(
    val subtitle: String?,
    val description: String?,
    val contentText: String,
)
