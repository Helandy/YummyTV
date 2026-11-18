package su.afk.yummy.tv.feature.library.model

import androidx.compose.runtime.Immutable
import su.afk.yummy.tv.domain.account.model.UserAnimeList

@Immutable
data class ListTransferStatusCountUi(
    val list: UserAnimeList,
    val count: Int,
)
