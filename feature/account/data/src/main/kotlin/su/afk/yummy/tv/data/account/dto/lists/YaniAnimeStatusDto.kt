package su.afk.yummy.tv.data.account.dto.lists

import kotlinx.serialization.Serializable

/** Статус выхода тайтла: `alias` — `ongoing` / `released` / `announcement`. */
@Serializable
data class YaniAnimeStatusDto(
    val value: Int? = null,
    val alias: String? = null,
)
