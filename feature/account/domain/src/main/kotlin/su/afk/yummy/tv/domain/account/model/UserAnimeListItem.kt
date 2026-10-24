package su.afk.yummy.tv.domain.account.model

import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus
import su.afk.yummy.tv.core.model.anime.AnimeSeason

data class UserAnimeListItem(
    val animeId: Int,
    val title: String,
    val posterUrl: String?,
    val poster: AnimePoster? = null,
    val rating: Double?,
    val year: Int?,
    val list: UserAnimeList?,
    val isFavorite: Boolean,
    val updatedAtSeconds: Long? = null,
    val userRating: Int? = null,
    /** Дата выхода следующей серии, epoch-секунды. */
    val nextEpisodeAtSeconds: Long? = null,
    /** Сезон выхода (квартал года). */
    val season: AnimeSeason? = null,
    /** Короткое имя типа тайтла с сервера («ТВ», «OVA», «п/ф»). */
    val kind: String? = null,
    val releaseStatus: AnimeReleaseStatus? = null,
)
