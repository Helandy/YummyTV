package su.afk.yummy.tv.data.details.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Картинки серии из Jikan-совместимого API `/anime/{malId}/episodes`; у свежих серий их может не быть. */
@Serializable
data class MalEpisodeImagesDto(
    val jpg: MalEpisodeImageDto = MalEpisodeImageDto(),
)

@Serializable
data class MalEpisodeImageDto(
    @SerialName("image_url") val imageUrl: String? = null,
)
