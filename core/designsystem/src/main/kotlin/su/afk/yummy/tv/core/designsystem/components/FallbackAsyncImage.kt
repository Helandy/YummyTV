package su.afk.yummy.tv.core.designsystem.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * Картинка с цепочкой запасных моделей: если текущая не загрузилась (битая версия на сервере,
 * 404, обрыв), берётся следующая из [models] (ссылка, Kodik-превью и т.п.). Одиночный
 * [AsyncImage] после ошибки остаётся пустым.
 */
@Composable
fun FallbackAsyncImage(
    models: List<Any>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    alpha: Float = DefaultAlpha,
) {
    var index by remember(models) { mutableIntStateOf(0) }
    AsyncImage(
        model = models.getOrNull(index),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        onError = { if (index < models.lastIndex) index++ },
    )
}
