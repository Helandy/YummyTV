package su.afk.yummy.tv.domain.library.usecase

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import su.afk.yummy.tv.core.common.coroutines.AppClock
import su.afk.yummy.tv.core.common.coroutines.runSuspendCatching
import su.afk.yummy.tv.core.model.anime.defaultUrl
import su.afk.yummy.tv.core.model.error.isRetryableMutationFailure
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.account.repository.AnimeExtrasRepository
import su.afk.yummy.tv.domain.account.repository.UserListsRepository
import su.afk.yummy.tv.domain.library.model.AnimeListImportOptions
import su.afk.yummy.tv.domain.library.model.AnimeListImportPhase
import su.afk.yummy.tv.domain.library.model.AnimeListImportProgress
import su.afk.yummy.tv.domain.library.model.AnimeListImportResult
import su.afk.yummy.tv.domain.library.model.ExportableLibraryAnime
import su.afk.yummy.tv.domain.library.model.ImportConflictPolicy
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.model.ParsedExternalList
import su.afk.yummy.tv.domain.library.model.ResolvedExternalAnime
import su.afk.yummy.tv.domain.library.model.UnmatchedExternalAnime
import su.afk.yummy.tv.domain.library.repository.AnimeListTransferRepository
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.utils.computeImportRetryDelayMs
import su.afk.yummy.tv.domain.library.utils.normalizedExternalScore
import su.afk.yummy.tv.domain.library.utils.selectPrimaryCandidate
import su.afk.yummy.tv.domain.library.utils.supportsLocalEpisodeImport
import javax.inject.Inject

/**
 * Импортирует разобранный список аниме (Shikimori / MyAnimeList) в библиотеку пользователя:
 * пакетно сопоставляет внешние ID с каталогом yani, вычисляет дельту относительно текущего списка
 * и последовательно применяет изменения с троттлингом и повторами при HTTP 429 / 5xx.
 */
class ImportAnimeListUseCase @Inject constructor(
    private val transferRepository: AnimeListTransferRepository,
    private val libraryRepository: LibraryRepository,
    private val userListsRepository: UserListsRepository,
    private val animeExtrasRepository: AnimeExtrasRepository,
    private val clock: AppClock,
) {
    suspend operator fun invoke(
        parsed: ParsedExternalList,
        options: AnimeListImportOptions,
        mutationDelayMs: Long = DEFAULT_MUTATION_DELAY_MS,
        isCancelled: () -> Boolean = { false },
        onProgress: suspend (AnimeListImportProgress) -> Unit = {},
    ): AnimeListImportResult {
        val entries = parsed.entries
        if (entries.isEmpty()) {
            return AnimeListImportResult(
                addedCount = 0,
                updatedCount = 0,
                skippedExistingCount = 0,
                unmatched = emptyList(),
                failedCount = 0,
            )
        }

        val externalIds = entries.map { it.externalId }.distinct()
        onProgress(
            AnimeListImportProgress(
                phase = AnimeListImportPhase.RESOLVING_TITLES,
                processed = 0,
                total = externalIds.size,
            ),
        )
        val resolvedByExternalId = transferRepository.resolveByExternalIds(
            format = parsed.format,
            externalIds = externalIds,
        ) { processedIds, totalIds ->
            onProgress(
                AnimeListImportProgress(
                    phase = AnimeListImportPhase.RESOLVING_TITLES,
                    processed = processedIds,
                    total = totalIds,
                ),
            )
        }

        if (isCancelled()) {
            return emptyCancelledResult()
        }

        onProgress(
            AnimeListImportProgress(
                phase = AnimeListImportPhase.LOADING_CURRENT_LIST,
                processed = 0,
                total = 1,
            ),
        )
        val userId = transferRepository.currentUserId()
        val isSignedIn = userId > 0
        val localByAnimeId = libraryRepository.getAll()
            .associateBy(LibraryItem::animeId)
            .toMutableMap()
        val remoteByAnimeId: Map<Int, ExportableLibraryAnime> = if (isSignedIn) {
            transferRepository.loadExportableLibrary().associateBy(ExportableLibraryAnime::animeId)
        } else {
            emptyMap()
        }

        val currentListByAnimeId = mutableMapOf<Int, UserAnimeList?>()
        val currentRatingByAnimeId = mutableMapOf<Int, Int?>()
        val favoriteByAnimeId = mutableMapOf<Int, Boolean>()
        (localByAnimeId.keys + remoteByAnimeId.keys).forEach { animeId ->
            val local = localByAnimeId[animeId]
            val remote = remoteByAnimeId[animeId]
            currentListByAnimeId[animeId] = remote?.list ?: local?.listId?.toUserAnimeListOrNull()
            currentRatingByAnimeId[animeId] = remote?.userRating.normalizedExternalScore()
                ?: local?.userRating.normalizedExternalScore()
            favoriteByAnimeId[animeId] = remote?.isFavorite == true || local?.isFavorite == true
        }
        val knownAnimeIds = currentListByAnimeId.keys.toMutableSet()

        var addedCount = 0
        var updatedCount = 0
        var skippedExistingCount = 0
        var failedCount = 0
        val unmatched = mutableListOf<UnmatchedExternalAnime>()
        var hasSentRemoteMutation = false

        onProgress(
            AnimeListImportProgress(
                phase = AnimeListImportPhase.APPLYING_CHANGES,
                processed = 0,
                total = entries.size,
            ),
        )

        for ((index, entry) in entries.withIndex()) {
            currentCoroutineContext().ensureActive()
            if (isCancelled()) {
                return AnimeListImportResult(
                    addedCount = addedCount,
                    updatedCount = updatedCount,
                    skippedExistingCount = skippedExistingCount,
                    unmatched = unmatched,
                    failedCount = failedCount,
                    wasCancelled = true,
                )
            }

            val candidate = resolvedByExternalId[entry.externalId]
                .orEmpty()
                .selectPrimaryCandidate(knownAnimeIds)
            if (candidate == null) {
                unmatched += UnmatchedExternalAnime(
                    externalId = entry.externalId,
                    title = entry.title,
                    list = entry.list,
                )
                onProgress(applyingProgress(index + 1, entries.size))
                continue
            }

            val animeId = candidate.animeId
            val currentList = currentListByAnimeId[animeId]
            val currentRating = currentRatingByAnimeId[animeId]
            val wasInList = currentList != null

            if (wasInList && options.conflictPolicy == ImportConflictPolicy.SKIP_EXISTING) {
                skippedExistingCount++
                onProgress(applyingProgress(index + 1, entries.size))
                continue
            }

            val needListUpdate = currentList != entry.list
            val targetScore = if (options.importScores) entry.score.normalizedExternalScore() else null
            val needScoreUpdate = targetScore != null && currentRating != targetScore
            val shouldMarkLocalEpisodes = options.importWatchedEpisodesLocally &&
                entry.watchedEpisodes > 0 &&
                entry.list.supportsLocalEpisodeImport()

            if (!needListUpdate && !needScoreUpdate) {
                if (shouldMarkLocalEpisodes) {
                    markLocalEpisodesSafely(candidate, entry.watchedEpisodes)
                }
                skippedExistingCount++
                onProgress(applyingProgress(index + 1, entries.size))
                continue
            }

            var appliedRating = currentRating
            var ratingFailed = false

            if (isSignedIn) {
                if (needListUpdate) {
                    if (hasSentRemoteMutation && mutationDelayMs > 0L) {
                        delay(mutationDelayMs)
                    }
                    hasSentRemoteMutation = true
                    val listApplied = executeWithRetry(isCancelled) {
                        userListsRepository.setAnimeList(animeId, entry.list)
                    }
                    if (!listApplied) {
                        if (isCancelled()) {
                            return AnimeListImportResult(
                                addedCount = addedCount,
                                updatedCount = updatedCount,
                                skippedExistingCount = skippedExistingCount,
                                unmatched = unmatched,
                                failedCount = failedCount,
                                wasCancelled = true,
                            )
                        }
                        failedCount++
                        onProgress(applyingProgress(index + 1, entries.size))
                        continue
                    }
                }

                if (needScoreUpdate) {
                    if (hasSentRemoteMutation && mutationDelayMs > 0L) {
                        delay(mutationDelayMs)
                    }
                    hasSentRemoteMutation = true
                    val scoreApplied = executeWithRetry(isCancelled) {
                        animeExtrasRepository.setRating(animeId, targetScore)
                    }
                    if (scoreApplied) {
                        appliedRating = targetScore
                    } else if (!isCancelled()) {
                        ratingFailed = true
                    }
                }
            } else if (needScoreUpdate) {
                appliedRating = targetScore
            }

            val now = clock.nowMillis()
            val updatedItem = buildMergedLibraryItem(
                candidate = candidate,
                previous = localByAnimeId[animeId],
                list = entry.list,
                isFavorite = favoriteByAnimeId[animeId] == true,
                userRating = appliedRating,
                now = now,
            )
            libraryRepository.add(updatedItem)
            localByAnimeId[animeId] = updatedItem
            currentListByAnimeId[animeId] = entry.list
            currentRatingByAnimeId[animeId] = appliedRating
            knownAnimeIds += animeId

            if (shouldMarkLocalEpisodes) {
                markLocalEpisodesSafely(candidate, entry.watchedEpisodes)
            }

            if (wasInList) updatedCount++ else addedCount++
            if (ratingFailed) failedCount++
            onProgress(applyingProgress(index + 1, entries.size))
        }

        if (isSignedIn && failedCount == 0 && (addedCount > 0 || updatedCount > 0)) {
            libraryRepository.markSynced(userId)
        }

        return AnimeListImportResult(
            addedCount = addedCount,
            updatedCount = updatedCount,
            skippedExistingCount = skippedExistingCount,
            unmatched = unmatched,
            failedCount = failedCount,
            wasCancelled = false,
        )
    }

    private suspend fun executeWithRetry(
        isCancelled: () -> Boolean,
        block: suspend () -> Unit,
    ): Boolean {
        for (attempt in 0..MAX_RETRY_ATTEMPTS) {
            if (isCancelled()) return false
            val error = runSuspendCatching { block() }.exceptionOrNull() ?: return true
            val canRetry = attempt < MAX_RETRY_ATTEMPTS && error.isRetryableMutationFailure()
            if (!canRetry || isCancelled()) return false
            delay(error.computeImportRetryDelayMs(attempt))
        }
        return false
    }

    private suspend fun markLocalEpisodesSafely(
        candidate: ResolvedExternalAnime,
        watchedEpisodes: Int,
    ) {
        runSuspendCatching {
            transferRepository.markEpisodesWatchedLocally(
                animeId = candidate.animeId,
                animeTitle = candidate.title,
                posterUrl = candidate.poster.defaultUrl().orEmpty(),
                watchedEpisodes = watchedEpisodes,
            )
        }
    }

    private companion object {
        const val DEFAULT_MUTATION_DELAY_MS = 350L
        const val MAX_RETRY_ATTEMPTS = 3

        fun emptyCancelledResult(): AnimeListImportResult =
            AnimeListImportResult(
                addedCount = 0,
                updatedCount = 0,
                skippedExistingCount = 0,
                unmatched = emptyList(),
                failedCount = 0,
                wasCancelled = true,
            )

        fun applyingProgress(processed: Int, total: Int): AnimeListImportProgress =
            AnimeListImportProgress(
                phase = AnimeListImportPhase.APPLYING_CHANGES,
                processed = processed,
                total = total,
            )

        fun Int.toUserAnimeListOrNull(): UserAnimeList? =
            UserAnimeList.entries.firstOrNull { it.id == this }

        fun buildMergedLibraryItem(
            candidate: ResolvedExternalAnime,
            previous: LibraryItem?,
            list: UserAnimeList,
            isFavorite: Boolean,
            userRating: Int?,
            now: Long,
        ): LibraryItem =
            LibraryItem(
                animeId = candidate.animeId,
                title = candidate.title.ifBlank { previous?.title.orEmpty() },
                poster = candidate.poster ?: previous?.poster,
                addedAt = previous?.addedAt?.takeIf { it > 0L } ?: now,
                listId = list.id,
                isFavorite = isFavorite,
                listUpdatedAt = now,
                favoriteUpdatedAt = previous?.favoriteUpdatedAt
                    ?: if (isFavorite) now else 0L,
                userRating = userRating,
                year = candidate.year ?: previous?.year,
                rating = candidate.rating ?: previous?.rating,
                nextEpisodeAtSeconds = previous?.nextEpisodeAtSeconds,
                season = candidate.season ?: previous?.season,
                kind = candidate.kind ?: previous?.kind,
                releaseStatus = candidate.releaseStatus ?: previous?.releaseStatus,
            )
    }
}
