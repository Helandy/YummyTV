package su.afk.yummy.tv.feature.main.model

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Размеры боковой панели ТВ: на низких экранах строки и отступы сжимаются, чтобы всё влезло. */
internal data class TvSideMenuMetrics(
    val itemHeight: Dp,
    val itemSpacing: Dp,
    val verticalPadding: Dp,
    /** Зазор между аккаунтом и пунктами меню. */
    val accountGap: Dp,
) {
    companion object {
        val Regular = TvSideMenuMetrics(itemHeight = 48.dp, itemSpacing = 4.dp, verticalPadding = 16.dp, accountGap = 8.dp)
        val Compact = TvSideMenuMetrics(itemHeight = 40.dp, itemSpacing = 2.dp, verticalPadding = 8.dp, accountGap = 4.dp)
    }
}
