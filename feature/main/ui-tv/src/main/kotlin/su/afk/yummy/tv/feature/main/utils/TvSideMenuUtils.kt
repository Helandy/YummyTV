package su.afk.yummy.tv.feature.main.utils

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.main.model.TvSideMenuMetrics

/**
 * Обычным размерам панели нужно около 512dp высоты; с запасом под системные отступы ниже
 * [CompactBelow] берём компактные. Если не помещаются и они — пункты прокручиваются.
 */
internal fun tvSideMenuMetrics(screenHeight: Dp): TvSideMenuMetrics =
    if (screenHeight < CompactBelow) TvSideMenuMetrics.Compact else TvSideMenuMetrics.Regular

private val CompactBelow = 540.dp
