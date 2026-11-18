package su.afk.yummy.tv.domain.library.usecase

import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.domain.library.model.AnimeListExportResult
import su.afk.yummy.tv.domain.library.model.EmptyExportListException
import su.afk.yummy.tv.domain.library.model.ExternalListEntry
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import su.afk.yummy.tv.domain.library.utils.buildExportFileName
import su.afk.yummy.tv.domain.library.utils.deduplicateExternalEntries
import su.afk.yummy.tv.domain.library.utils.externalIdFor
import su.afk.yummy.tv.domain.library.utils.normalizedExternalScore
import su.afk.yummy.tv.domain.library.utils.resolveExportedWatchedEpisodes
import su.afk.yummy.tv.domain.library.utils.toMalSeriesType
import javax.inject.Inject

/**
 * Собирает библиотеку пользователя (статусы, оценки и локально отмеченные серии) и сериализует
 * её в формат MyAnimeList XML или Shikimori JSON без поштучных запросов к `/anime/{id}/videos`.
 */
class ExportAnimeListUseCase @Inject constructor(
    private val repository: AnimeListTransferRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(format: ExternalListFormat): AnimeListExportResult {
        val library = repository.loadExportableLibrary()
        val watchedCounts = repository.loadLocalWatchedEpisodeCounts()
        val userName = repository.currentNickname()

        var skippedWithoutExternalId = 0
        var skippedFavoriteOnly = 0
        val exportEntries = ArrayList<ExternalListEntry>(library.size)

        library.forEach { item ->
            val list = item.list
            if (list == null) {
                if (item.isFavorite) skippedFavoriteOnly++
                return@forEach
            }
            val externalId = item.externalIdFor(format)
            if (externalId == null) {
                skippedWithoutExternalId++
                return@forEach
            }
            val watchedEpisodes = resolveExportedWatchedEpisodes(
                list = list,
                typeAlias = item.typeAlias,
                localWatchedCount = watchedCounts[item.animeId] ?: 0,
            )
            exportEntries += ExternalListEntry(
                externalId = externalId,
                title = item.title,
                list = list,
                score = item.userRating.normalizedExternalScore(),
                watchedEpisodes = watchedEpisodes,
                rewatches = 0,
                seriesType = item.typeAlias.toMalSeriesType(),
                totalEpisodes = 0,
            )
        }

        val deduplicated = exportEntries.deduplicateExternalEntries()
        if (deduplicated.isEmpty()) {
            throw EmptyExportListException(
                skippedWithoutExternalIdCount = skippedWithoutExternalId,
                skippedFavoriteOnlyCount = skippedFavoriteOnly,
            )
        }

        val content = repository.formatExternalList(
            format = format,
            userName = userName,
            entries = deduplicated,
        )
        return AnimeListExportResult(
            format = format,
            content = content,
            suggestedFileName = buildExportFileName(format, clock.nowMillis()),
            exportedCount = deduplicated.size,
            skippedWithoutExternalIdCount = skippedWithoutExternalId,
            skippedFavoriteOnlyCount = skippedFavoriteOnly,
        )
    }
}
