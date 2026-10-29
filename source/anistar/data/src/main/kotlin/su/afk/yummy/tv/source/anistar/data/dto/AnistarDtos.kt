package su.afk.yummy.tv.source.anistar.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class AnistarMirrorResponseDto(
    val mirror: String? = null,
)
