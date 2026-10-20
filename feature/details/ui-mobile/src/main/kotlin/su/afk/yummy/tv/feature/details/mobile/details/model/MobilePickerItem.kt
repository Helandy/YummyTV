package su.afk.yummy.tv.feature.details.mobile.details.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
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
    /** Логотип студии/балансера слева; общая иконка, если [logoRes] нет. Без [logoName] не рисуется. */
    val logoName: String? = null,
    val logoRes: Int? = null,
    /** Иконка вместо логотипа, когда его нет: у озвучек своя, у плееров своя. */
    val logoFallbackIcon: ImageVector = Icons.Filled.PlayCircle,
    val enabled: Boolean = true,
    /** Название пункта акцентным цветом — как в шторках скачивания. */
    val accentTitle: Boolean = false,
    /** Подзаголовок крупнее — когда это озвучка, а не список балансеров. */
    val emphasizedSubtitle: Boolean = false,
    /** Слот справа, по центру строки: статус скачивания и т.п. */
    val trailing: (@Composable () -> Unit)? = null,
    val onClick: () -> Unit,
)
