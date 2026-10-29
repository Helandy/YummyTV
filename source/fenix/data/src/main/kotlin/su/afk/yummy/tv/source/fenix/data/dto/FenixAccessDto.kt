package su.afk.yummy.tv.source.fenix.data.dto

import kotlinx.serialization.Serializable

/** `window.FENIX_MEDIA_ACCESS` страницы плеера: токен доступа к медиа-хосту, ограниченный по пути и времени. */
@Serializable
internal data class FenixAccessDto(
    val token: String? = null,
    val edge: FenixEdgeDto? = null,
)

/** Запасной медиа-хост страницы (`edge`): свой origin и свой токен, который уходит в query как `edge=`. */
@Serializable
internal data class FenixEdgeDto(
    val origin: String? = null,
    val token: String? = null,
)
