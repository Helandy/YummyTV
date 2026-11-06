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
import coil3.network.HttpException
import coil3.request.ImageRequest
import su.afk.yummy.tv.core.model.anime.utils.isPosterMirrorUrl
import java.io.IOException

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
        onError = { state ->
            val next = models.getOrNull(index + 1)
            if (next != null && shouldFallback(next, state.result.throwable)) index++
        },
    )
}

/**
 * К копии постера на нашем хосте переходим, когда yani ответил 404 или 5xx, а также при сетевом сбое (DNS, соединение,
 * таймаут): картинки не будет ни с какой другой стороны, а копия лежит на своём узле. Ошибки не про сеть (например,
 * не декодируется картинка) не повод слать лишние запросы на свой узел. К остальным запасным моделям (Kodik-превью и т.п.)
 * идём при любой ошибке.
 */
private fun shouldFallback(next: Any, error: Throwable): Boolean {
    if (next !is String || !isPosterMirrorUrl(next)) return true
    if (error is HttpException) return error.response.code.let { it == 404 || it in 500..599 }
    return error is IOException
}
