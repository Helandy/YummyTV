package su.afk.yummy.tv.core.designsystem.mobile.tabs

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** Вкладка-чип для [MobileChipTabs]: подпись, иконка, счётчик и цвет акцента. */
@Immutable
data class MobileChipTab<K>(
    val key: K,
    val title: String,
    val icon: ImageVector?,
    val count: Int?,
    val color: Color,
)
