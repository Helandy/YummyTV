package su.afk.yummy.tv.core.model.anime

import su.afk.yummy.tv.core.model.anime.utils.normalizeYaniStaticUrl
import su.afk.yummy.tv.core.model.anime.utils.posterMirrorUrl
import su.afk.yummy.tv.core.model.anime.utils.yaniStaticPosterFallback

data class AnimePoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

/**
 * Ссылки на постер для показа в UI, «водопадом»: версия `mega` из API (у устройств с AVIF это AVIF, у остальных yani
 * подставляет в неё полный JPEG), затем тот же путь на `static.yani.tv` ([yaniStaticPosterFallback], если API отдал
 * другой хост картинок), затем копия на нашем хосте ([posterMirrorUrl]; для JPEG это тот же постер в виде AVIF). К следующей ссылке картинка
 * переходит при ошибке предыдущей. Пусто, если `mega` нет.
 */
fun AnimePoster?.posterUrls(): List<String> {
    val url = this?.mega?.takeIf { it.isNotBlank() }?.let(::normalizeYaniStaticUrl) ?: return emptyList()
    return listOfNotNull(url, yaniStaticPosterFallback(url), posterMirrorUrl(url))
}

/** Ссылка на постер для записи строкой: версия `mega` (см. [posterUrls]); `null`, если её нет. */
fun AnimePoster?.defaultUrl(): String? = this?.mega?.takeIf { it.isNotBlank() }?.let(::normalizeYaniStaticUrl)
