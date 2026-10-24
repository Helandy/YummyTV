package su.afk.yummy.tv.android.continuewatching

import android.content.Context
import android.graphics.Bitmap
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.toBitmap
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.bestUrl
import su.afk.yummy.tv.domain.home.model.HomeContinueWatchingItem

/** Постер для ярлыков и виджета: средний размер достаточен, крупные версии только тратят сеть. */
internal fun HomeContinueWatchingItem.surfacePosterUrl(): String? = poster?.medium ?: poster.bestUrl()

/**
 * Постер software-bitmap'ом через синглтон Coil (общий дисковый кэш с приложением).
 * Hardware-bitmap не годится: ни иконка ярлыка, ни RemoteViews виджета его не сериализуют.
 */
internal suspend fun loadSoftwarePoster(context: Context, url: String, sizePx: Int): Bitmap? =
    runSuspendCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(sizePx)
            .allowHardware(false)
            .build()
        (context.imageLoader.execute(request) as? SuccessResult)?.image?.toBitmap()
    }.getOrNull()
