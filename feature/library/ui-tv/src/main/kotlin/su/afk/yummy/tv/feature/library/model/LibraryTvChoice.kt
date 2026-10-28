package su.afk.yummy.tv.feature.library.model

import androidx.compose.ui.graphics.vector.ImageVector

/** Пункт ТВ-оверлея выбора: [selected] отмечает текущее значение, [destructive] — опасное действие. */
internal data class LibraryTvChoice(
    val label: String,
    val icon: ImageVector? = null,
    val selected: Boolean = false,
    val destructive: Boolean = false,
)
