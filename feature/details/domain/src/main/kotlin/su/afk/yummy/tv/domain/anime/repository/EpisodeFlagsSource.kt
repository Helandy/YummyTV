package su.afk.yummy.tv.domain.anime.repository

import su.afk.yummy.tv.core.model.anime.EpisodeFlags

/**
 * Точка расширения: внешний источник пометок «филлер/рекап» по MyAnimeList id.
 * Без подключённого модуля набор реализаций пуст, пометки берутся из резерва на YummyTV API.
 */
interface EpisodeFlagsSource {
    /**
     * Пометки всех серий тайтла: ключ — номер серии строкой, серии без пометок опускаются. [onFirstPage] получает пометки
     * первой страницы, пока качаются остальные (только если источник отдаёт список постранично). Любая ошибка пробрасывается:
     * неполный список в кэш попасть не должен.
     */
    suspend fun getEpisodeFlags(
        malId: Int,
        onFirstPage: suspend (Map<String, EpisodeFlags>) -> Unit = {},
    ): Map<String, EpisodeFlags>
}
