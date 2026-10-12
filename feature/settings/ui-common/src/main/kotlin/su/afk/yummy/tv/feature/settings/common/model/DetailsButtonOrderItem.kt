package su.afk.yummy.tv.feature.settings.common.model

import su.afk.yummy.tv.core.model.settings.DetailsButtonAction

data class DetailsButtonOrderItem(
    val key: String,
    val action: DetailsButtonAction,
    val label: String,
)
