package su.afk.yummy.tv.feature.details.details.utils

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/** Верх [child] в координатах этого узла: не зависит от прокрутки ленты, в отличие от positionInRoot. */
internal fun LayoutCoordinates.localTopOf(child: LayoutCoordinates): Float = localPositionOf(child, Offset.Zero).y
