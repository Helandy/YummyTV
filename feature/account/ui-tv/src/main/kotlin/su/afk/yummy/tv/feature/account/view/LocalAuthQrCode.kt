package su.afk.yummy.tv.feature.account.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import su.afk.yummy.tv.feature.account.utils.encodeQrMatrix

/**
 * QR сканируется камерой телефона с экрана ТВ: цвета жёстко чёрный по белому независимо от темы,
 * иначе на тёмной теме контраст и «тихая зона» теряются.
 */
@Composable
internal fun LocalAuthQrCode(
    content: String,
    modifier: Modifier = Modifier,
    size: Dp = 220.dp,
) {
    val matrix = remember(content) { encodeQrMatrix(content) }

    Box(
        modifier = modifier
            .size(size)
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(8.dp),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .aspectRatio(1f)
        ) {
            val cell = minOf(this.size.width / matrix.width, this.size.height / matrix.height)
            // Заливаем на полпикселя шире, чтобы между соседними модулями не было светлых швов.
            val cellSize = Size(cell + 0.5f, cell + 0.5f)
            for (y in 0 until matrix.height) {
                for (x in 0 until matrix.width) {
                    if (matrix[x, y]) {
                        drawRect(
                            color = Color.Black,
                            topLeft = Offset(x * cell, y * cell),
                            size = cellSize,
                        )
                    }
                }
            }
        }
    }
}
