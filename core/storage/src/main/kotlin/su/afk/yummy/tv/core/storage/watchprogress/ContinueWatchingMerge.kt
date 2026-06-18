package su.afk.yummy.tv.core.storage.watchprogress

object ContinueWatchingMerge {

    fun merge(
        feedEntries: List<WatchProgressEntry>,
        localEntries: List<WatchProgressEntry>,
    ): List<WatchProgressEntry> {
        val result = linkedMapOf<Int, WatchProgressEntry>()
        feedEntries.forEach { entry ->
            if (entry.animeId <= 0) return@forEach
            val current = result[entry.animeId]
            if (current == null || entry.updatedAt > current.updatedAt) {
                result[entry.animeId] = entry
            }
        }

        bestByAnime(localEntries).forEach { local ->
            val current = result[local.animeId]
            if (current == null || local.updatedAt > current.updatedAt) {
                result[local.animeId] = local
            }
        }

        return result.values.sortedByDescending { it.updatedAt }
    }

    fun bestByAnime(entries: List<WatchProgressEntry>): List<WatchProgressEntry> {
        val result = linkedMapOf<Int, WatchProgressEntry>()
        entries.forEach { entry ->
            if (entry.animeId <= 0) return@forEach
            val current = result[entry.animeId]
            if (current == null || isFurtherThan(entry, current)) {
                result[entry.animeId] = entry
            }
        }
        return result.values.sortedByDescending { it.updatedAt }
    }

    fun isFurtherThan(
        entry: WatchProgressEntry,
        other: WatchProgressEntry,
    ): Boolean {
        if (entry.animeId <= 0 || entry.animeId != other.animeId) return false

        val episodeNumber = entry.episode.episodeNumberOrNull()
        val otherEpisodeNumber = other.episode.episodeNumberOrNull()
        if (episodeNumber != null && otherEpisodeNumber != null) {
            val episodeComparison = episodeNumber.compareTo(otherEpisodeNumber)
            if (episodeComparison != 0) return episodeComparison > 0
        }

        val entryIsTarget = WatchProgressStore.isContinueTargetEntry(entry)
        val otherIsTarget = WatchProgressStore.isContinueTargetEntry(other)
        if (entryIsTarget != otherIsTarget) {
            val updatedComparison = entry.updatedAt.compareTo(other.updatedAt)
            if (updatedComparison != 0) return updatedComparison > 0
        }

        val progressComparison = entry.progressScore().compareTo(other.progressScore())
        if (progressComparison != 0) return progressComparison > 0

        val positionComparison = entry.positionMs.compareTo(other.positionMs)
        if (positionComparison != 0) return positionComparison > 0

        return entry.updatedAt > other.updatedAt
    }

    private fun WatchProgressEntry.progressScore(): Float =
        WatchProgressStore.progress(positionMs, durationMs)

    private fun String.episodeNumberOrNull(): Double? {
        val normalized = trim().replace(',', '.')
        return normalized.toDoubleOrNull()
            ?: Regex("""\d+(?:[.,]\d+)?""")
                .find(normalized)
                ?.value
                ?.replace(',', '.')
                ?.toDoubleOrNull()
    }
}
