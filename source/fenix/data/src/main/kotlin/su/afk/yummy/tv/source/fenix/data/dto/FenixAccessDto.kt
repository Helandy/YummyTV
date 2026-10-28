package su.afk.yummy.tv.source.fenix.data.dto

import kotlinx.serialization.Serializable

/** `window.FENIX_MEDIA_ACCESS` страницы плеера: токен доступа к медиа-хосту, ограниченный по пути и времени. */
@Serializable
internal data class FenixAccessDto(
    val token: String? = null,
)
