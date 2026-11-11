package su.afk.yummy.tv.core.designsystem.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Значок «торрент» — магнит (торренты ищутся по magnet-ссылкам). Контур совпадает с `dub_torrent.xml`. */
val TorrentIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Torrent",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Полюса магнита: отделены от дуги зазором, иначе в одном цвете значок читается как буква «U».
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 5.5f)
            curveTo(5f, 4.67f, 5.67f, 4f, 6.5f, 4f)
            horizontalLineToRelative(2f)
            curveTo(9.33f, 4f, 10f, 4.67f, 10f, 5.5f)
            verticalLineTo(8f)
            horizontalLineTo(5f)
            close()
            moveTo(14f, 5.5f)
            curveTo(14f, 4.67f, 14.67f, 4f, 15.5f, 4f)
            horizontalLineToRelative(2f)
            curveTo(18.33f, 4f, 19f, 4.67f, 19f, 5.5f)
            verticalLineTo(8f)
            horizontalLineToRelative(-5f)
            close()
        }
        // Дуга магнита.
        path(fill = SolidColor(Color.Black)) {
            moveTo(5f, 9.5f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(2f)
            curveTo(10f, 12.88f, 10.9f, 14f, 12f, 14f)
            reflectiveCurveToRelative(2f, -1.12f, 2f, -2.5f)
            verticalLineToRelative(-2f)
            horizontalLineToRelative(5f)
            verticalLineToRelative(2f)
            curveTo(19f, 15.09f, 15.87f, 18f, 12f, 18f)
            reflectiveCurveToRelative(-7f, -2.91f, -7f, -6.5f)
            close()
        }
    }.build()
}
