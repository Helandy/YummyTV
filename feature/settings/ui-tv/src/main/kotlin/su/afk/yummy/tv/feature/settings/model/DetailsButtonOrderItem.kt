package su.afk.yummy.tv.feature.settings.model

import su.afk.yummy.tv.core.preferences.settings.DetailsButtonAction

internal data class DetailsButtonOrderItem(
    val key: String,
    val action: DetailsButtonAction,
    val label: String,
)
