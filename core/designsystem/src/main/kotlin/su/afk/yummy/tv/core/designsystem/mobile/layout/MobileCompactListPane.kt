package su.afk.yummy.tv.core.designsystem.mobile.layout

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Размеры свёрнутой панели списка рядом с деталью (см. `CompactListPaneDestination`). */
object MobileCompactListPane {

    /** Ширина свёрнутой панели: аватарка 52dp и поля по бокам. */
    val Width: Dp = 88.dp

    /** Уже этой ширины список рисуется свёрнутым; обычная панель списка — от 360dp. */
    private val CompactThreshold: Dp = 160.dp

    fun isCompact(availableWidth: Dp): Boolean = availableWidth < CompactThreshold
}
