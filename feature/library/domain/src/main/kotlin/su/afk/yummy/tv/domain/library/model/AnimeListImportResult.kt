package su.afk.yummy.tv.domain.library.model

/** Итоговая сводка завершённого (или остановленного пользователем) импорта списка. */
data class AnimeListImportResult(
    val addedCount: Int,
    val updatedCount: Int,
    val skippedExistingCount: Int,
    val unmatched: List<UnmatchedExternalAnime>,
    val failedCount: Int,
    val lookupFailedCount: Int = 0,
    val failedEntries: List<ExternalListEntry> = emptyList(),
    val pendingScoreExternalIds: Set<Int> = emptySet(),
    val wasCancelled: Boolean = false,
)
