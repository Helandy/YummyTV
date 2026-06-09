package su.afk.yummy.tv.feature.main.model

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.navigation.root.RootTab

data class TvMenuItem(
    @param:StringRes val titleRes: Int,
    val destination: RootTab,
    val icon: ImageVector? = null,
)
