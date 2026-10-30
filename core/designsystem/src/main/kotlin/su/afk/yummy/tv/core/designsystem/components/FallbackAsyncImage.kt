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
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest

/**
 * Картинка с цепочкой запасных моделей: если текущая не загрузилась (битая версия на сервере,
 * 404, обрыв), берётся следующая из [models] (ссылка, Kodik-превью и т.п.). Одиночный
 * [AsyncImage] после ошибки остаётся пустым.
 *
 * [cacheKey] — стабильный ключ кэша для строковых моделей: все ссылки цепочки пишутся и читаются под одним ключом
 * (память и диск), поэтому картинка из любого источника при следующем показе берётся из кэша, а не из сети.
 */
@Composable
fun FallbackAsyncImage(
    models: List<Any>,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    alpha: Float = DefaultAlpha,
    cacheKey: String? = null,
) {
    var index by remember(models) { mutableIntStateOf(0) }
    val context = LocalPlatformContext.current
    val model = models.getOrNull(index)
    val request = remember(model, cacheKey, context) {
        if (cacheKey != null && model is String) {
            ImageRequest.Builder(context).data(model).memoryCacheKey(cacheKey).diskCacheKey(cacheKey).build()
        } else {
            model
        }
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
        alpha = alpha,
        onError = { if (index < models.lastIndex) index++ },
    )
}
