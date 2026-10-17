package su.afk.yummy.tv.feature.details.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import su.afk.yummy.tv.core.designsystem.components.icon
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.domain.account.model.UserAnimeList

@Composable
fun UserAnimeList.statusColor(): Color = when (this) {
    UserAnimeList.WATCHING -> YummySemanticColors.StatusWatching
    UserAnimeList.PLANNED -> YummySemanticColors.StatusPlanned
    UserAnimeList.COMPLETED -> YummySemanticColors.StatusCompleted
    UserAnimeList.POSTPONED -> YummySemanticColors.StatusPostponed
    UserAnimeList.DROPPED -> YummySemanticColors.StatusDropped
}

fun UserAnimeList.statusIcon(): ImageVector = when (this) {
    UserAnimeList.WATCHING -> CardListStatus.WATCHING
    UserAnimeList.PLANNED -> CardListStatus.PLANNED
    UserAnimeList.COMPLETED -> CardListStatus.COMPLETED
    UserAnimeList.POSTPONED -> CardListStatus.POSTPONED
    UserAnimeList.DROPPED -> CardListStatus.DROPPED
}.icon()
