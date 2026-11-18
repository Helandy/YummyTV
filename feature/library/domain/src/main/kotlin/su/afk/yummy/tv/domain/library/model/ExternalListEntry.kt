package su.afk.yummy.tv.domain.library.model

import su.afk.yummy.tv.domain.account.model.UserAnimeList

/** Запись списка аниме во внешнем формате (Shikimori / MyAnimeList). */
data class ExternalListEntry(
    /** Идентификатор тайтла на Shikimori или MyAnimeList (совпадает в обеих базах). */
    val externalId: Int,
    val title: String,
    val list: UserAnimeList,
    /** Пользовательская оценка `1..10` или `null`, если не выставлена. */
    val score: Int?,
    /** Число просмотренных серий (`>= 0`). */
    val watchedEpisodes: Int,
    /** Число повторных просмотров (`>= 0`). */
    val rewatches: Int = 0,
    /** Тип релиза в терминологии MAL (`TV`, `Movie`, `OVA`, `ONA`, `Special`, `Unknown`). */
    val seriesType: String? = null,
    /** Общее число серий тайтла, если известно из исходного файла (`0` — неизвестно). */
    val totalEpisodes: Int = 0,
)
