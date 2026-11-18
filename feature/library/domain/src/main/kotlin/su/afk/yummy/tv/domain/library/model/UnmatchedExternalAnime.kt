package su.afk.yummy.tv.domain.library.model

import su.afk.yummy.tv.domain.account.model.UserAnimeList

/** Запись внешнего списка, для которой не нашлось соответствия в каталоге YummyAnime. */
data class UnmatchedExternalAnime(
    val externalId: Int,
    val title: String,
    val list: UserAnimeList,
)
