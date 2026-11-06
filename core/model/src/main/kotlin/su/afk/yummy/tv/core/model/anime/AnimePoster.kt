package su.afk.yummy.tv.core.model.anime

import su.afk.yummy.tv.core.model.anime.utils.normalizeYaniStaticUrl
import su.afk.yummy.tv.core.model.anime.utils.posterMirrorUrl

data class AnimePoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)

/**
 * Ссылки на постер для показа в UI: версия `mega` (у устройств с AVIF это AVIF, у остальных yani подставляет в неё
 * полный JPEG) и, если это AVIF, следом копия на нашем хосте ([posterMirrorUrl]): к ней картинка переходит при
 * 404/5xx и сетевом сбое yani. Пусто, если `mega` нет.
 */
fun AnimePoster?.posterUrls(): List<String> {
    val url = this?.mega?.takeIf { it.isNotBlank() }?.let(::normalizeYaniStaticUrl) ?: return emptyList()
    return listOfNotNull(url, posterMirrorUrl(url))
}

/** Ссылка на постер для записи строкой: версия `mega` (см. [posterUrls]); `null`, если её нет. */
fun AnimePoster?.defaultUrl(): String? = this?.mega?.takeIf { it.isNotBlank() }?.let(::normalizeYaniStaticUrl)
