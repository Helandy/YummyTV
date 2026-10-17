package su.afk.yummy.tv.feature.details.mobile.details.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

internal data class MobilePickerItem(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val views: Int? = null,
    /** Число серий у озвучки — если задано, мета-строка рисуется как в плеере. */
    val episodeCount: Int? = null,
    val color: Color? = null,
    /** Иконка пункта (в цвете [color]); без неё рисуется цветная точка. */
    val icon: ImageVector? = null,
    val enabled: Boolean = true,
    /** Название пункта акцентным цветом — как в шторках скачивания. */
    val accentTitle: Boolean = false,
    /** Подзаголовок крупнее — когда это озвучка, а не список балансеров. */
    val emphasizedSubtitle: Boolean = false,
    /** Слот справа, по центру строки: статус скачивания и т.п. */
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
)
