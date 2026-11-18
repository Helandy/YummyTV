package su.afk.yummy.tv.domain.library.model

import su.afk.yummy.tv.core.model.anime.AnimePoster
import su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus
import su.afk.yummy.tv.core.model.anime.AnimeSeason

/** Тайтл из каталога YummyAnime, сопоставленный с внешним ID Shikimori / MyAnimeList. */
data class ResolvedExternalAnime(
    val externalId: Int,
    val animeId: Int,
    val title: String,
    val poster: AnimePoster? = null,
    val rating: Double? = null,
    val year: Int? = null,
    val season: AnimeSeason? = null,
    val kind: String? = null,
    val releaseStatus: AnimeReleaseStatus? = null,
)
