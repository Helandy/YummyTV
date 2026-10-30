package su.afk.yummy.tv.domain.home.usecase

import su.afk.yummy.tv.domain.home.model.HomeFeedItem
import su.afk.yummy.tv.domain.home.model.LibraryEpisodePush
import su.afk.yummy.tv.domain.home.model.LibraryEpisodePushSelection
import su.afk.yummy.tv.domain.home.utils.LIBRARY_EPISODE_BASELINE_KEY
import su.afk.yummy.tv.domain.home.utils.isEpisodeWatched
import su.afk.yummy.tv.domain.home.utils.libraryEpisodeKey
import javax.inject.Inject

/**
 * Выбирает серии, о которых нужно показать локальный пуш: недавно вышедшие серии тайтлов из
 * списков пользователя, которые он ещё не смотрел и о которых уже не уведомляли.
 *
 * Первый прогон (нет отметки базовой линии) только запоминает текущее состояние и ничего не
 * присылает. Дальше пуш получает серия, которая вышла не раньше [FRESH_WINDOW_SECONDS] назад и по
 * тайтлу которой не было пуша (серверного или локального) за последние [DEDUP_WINDOW_SECONDS].
 * Устаревшие серии молча помечаются известными, чтобы не всплыть при добавлении тайтла в список
 * или после долгого офлайна. Чистая функция: без сети и хранилищ.
 */
class SelectLibraryEpisodePushesUseCase @Inject constructor() {

    operator fun invoke(
        recentlyAired: List<HomeFeedItem>,
        libraryAnimeIds: Set<Int>,
        watchedEpisodes: Map<Int, Set<Int>>,
        knownKeys: Set<String>,
        recentPushes: Map<Int, Long>,
        nowSeconds: Long,
    ): LibraryEpisodePushSelection {
        val candidates = recentlyAired.filter { item ->
            item.id in libraryAnimeIds && item.episodeNumber != null && item.airedAtSeconds != null
        }
        val seenKeys = candidates.mapTo(mutableSetOf(LIBRARY_EPISODE_BASELINE_KEY)) { item ->
            libraryEpisodeKey(item.id, requireNotNull(item.episodeNumber))
        }
        if (LIBRARY_EPISODE_BASELINE_KEY !in knownKeys) {
            return LibraryEpisodePushSelection(pushes = emptyList(), knownKeys = seenKeys)
        }

        val freshThreshold = nowSeconds - FRESH_WINDOW_SECONDS
        val dedupThreshold = nowSeconds - DEDUP_WINDOW_SECONDS
        val pushes = candidates.mapNotNull { item ->
            val episode = requireNotNull(item.episodeNumber)
            val airedAt = requireNotNull(item.airedAtSeconds)
            val isNew = libraryEpisodeKey(item.id, episode) !in knownKeys
            val isPushable = isNew &&
                airedAt >= freshThreshold &&
                !item.isEpisodeWatched(watchedEpisodes) &&
                (recentPushes[item.id] ?: Long.MIN_VALUE) < dedupThreshold
            if (!isPushable) return@mapNotNull null
            LibraryEpisodePush(
                animeId = item.id,
                title = item.title,
                episodeNumber = episode,
                airedAtSeconds = airedAt,
            )
        }
        return LibraryEpisodePushSelection(pushes = pushes, knownKeys = seenKeys)
    }

    companion object {
        /** У части источников известен только день (полночь UTC), поэтому окно с запасом. */
        const val FRESH_WINDOW_SECONDS = 48L * 60 * 60

        /** Не чаще одного пуша на тайтл за это время — общий дедуп с серверным пушем. */
        const val DEDUP_WINDOW_SECONDS = 24L * 60 * 60
    }
}
