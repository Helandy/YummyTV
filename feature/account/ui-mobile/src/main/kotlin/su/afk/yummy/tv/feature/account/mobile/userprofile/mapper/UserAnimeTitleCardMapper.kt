package su.afk.yummy.tv.feature.account.mobile.userprofile.mapper

import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi
import su.afk.yummy.tv.core.model.anime.CardListStatus
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.model.UserAnimeListItem
import su.afk.yummy.tv.feature.account.mobile.account.utils.formatUserListDate
import su.afk.yummy.tv.feature.account.userprofile.UserProfileState

/**
 * Карточка тайтла из списка профиля. Как в библиотеке: список показываем только в фильтре
 * «Любимые», сердечко — только вне его. [untitled] подставляется вместо пустого названия.
 */
internal fun UserAnimeListItem.toTitleListCardUi(
    filter: UserProfileState.ListFilter,
    untitled: String,
): TitleListCardUi {
    val isFavoritesFilter = filter == UserProfileState.ListFilter.FAVORITES
    return TitleListCardUi(
        animeId = animeId,
        title = title.ifBlank { untitled },
        posterUrl = posterUrl,
        siteRating = rating?.takeIf { it > 0.0 },
        userRating = userRating?.takeIf { it in 1..10 },
        year = year,
        releaseStatus = releaseStatus,
        listStatus = if (isFavoritesFilter) list?.toCardListStatus() else null,
        isFavorite = isFavorite && !isFavoritesFilter,
        dateText = updatedAtSeconds?.formatUserListDate()?.takeIf { it.isNotBlank() },
        nextEpisodeAtSeconds = nextEpisodeAtSeconds,
    )
}

private fun UserAnimeList.toCardListStatus(): CardListStatus = when (this) {
    UserAnimeList.WATCHING -> CardListStatus.WATCHING
    UserAnimeList.PLANNED -> CardListStatus.PLANNED
    UserAnimeList.COMPLETED -> CardListStatus.COMPLETED
    UserAnimeList.POSTPONED -> CardListStatus.POSTPONED
    UserAnimeList.DROPPED -> CardListStatus.DROPPED
}
