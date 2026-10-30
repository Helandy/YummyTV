package su.afk.yummy.tv.data.details.dto

import kotlinx.serialization.Serializable

/** Кадр серии из TMDB; лучшие по оценке идут первыми. [url] — оригинал на CDN TMDB, [filePath] — его хвост (`/abc.jpg`), по нему собирается ссылка другого размера. */
@Serializable
data class YummyImageDto(
    val url: String = "",
    val filePath: String = "",
)
