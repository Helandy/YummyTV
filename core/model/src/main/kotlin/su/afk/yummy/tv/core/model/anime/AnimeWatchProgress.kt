package su.afk.yummy.tv.core.model.anime

import su.afk.yummy.tv.core.model.settings.WatchedThresholds

private const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L
private const val SHORT_EPISODE_WATCHED_PROGRESS = 0.90f

/**
 * Пользовательские пороги "просмотрено" для текущего процесса. Правило ниже вызывается
 * синхронно из storage/presentation, поэтому настройки не протаскиваются параметром, а
 * синхронизируются сюда из DataStore при старте приложения.
 */
object WatchedEpisodeRule {
    @Volatile
    var thresholds: WatchedThresholds = WatchedThresholds()
        private set

    fun update(thresholds: WatchedThresholds) {
        this.thresholds = thresholds.coerced()
    }
}

data class AnimeWatchProgress(
    val animeId: Int,
    val episode: String,
    val videoId: Int = 0,
    val episodeUrl: String,
    val positionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
    val animeTitle: String = "",
    val posterUrl: String = "",
    val playerName: String = "",
    val dubbing: String = "",
    val screenshotUrl: String = "",
)

/**
 * Порог и функции ниже определяют единственный источник правды для правил
 * "просмотрено" / Continue Watching — переиспользуются как в data-слое (например,
 * [su.afk.yummy.tv.core.storage] через маппинг Entity в [AnimeWatchProgress]), так и
 * напрямую в presentation, когда под рукой есть только сырые positionMs/durationMs.
 */
fun progress(positionMs: Long, durationMs: Long): Float =
    if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

fun isMeaningfulProgress(positionMs: Long, durationMs: Long): Boolean =
    durationMs > 0 && positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS

fun isWatchedProgress(positionMs: Long, durationMs: Long): Boolean {
    if (!isMeaningfulProgress(positionMs, durationMs)) return false
    val remainingMs = WatchedEpisodeRule.thresholds.remainingMsFor(durationMs)
    return if (durationMs <= remainingMs) {
        progress(positionMs, durationMs) >= SHORT_EPISODE_WATCHED_PROGRESS
    } else {
        positionMs >= durationMs - remainingMs
    }
}

fun isContinueTarget(positionMs: Long, durationMs: Long): Boolean =
    positionMs == 0L && durationMs == 0L

fun isUnresolvedProgress(positionMs: Long, durationMs: Long): Boolean =
    durationMs == 0L && positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS

fun AnimeWatchProgress.progress(): Float = progress(positionMs, durationMs)

fun AnimeWatchProgress.isMeaningfulProgress(): Boolean =
    isMeaningfulProgress(positionMs, durationMs)

fun AnimeWatchProgress.isContinueTarget(): Boolean =
    isContinueTarget(positionMs, durationMs) &&
            episode.isNotBlank() &&
            episodeUrl.isNotBlank()

fun AnimeWatchProgress.hasPlayableTarget(): Boolean =
    videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()

fun AnimeWatchProgress.isUnresolvedProgress(): Boolean =
    isUnresolvedProgress(positionMs, durationMs) && hasPlayableTarget()

fun AnimeWatchProgress.isWatchedProgress(): Boolean = isWatchedProgress(positionMs, durationMs)

fun AnimeWatchProgress.isContinueWatchingProgress(): Boolean =
    isContinueTarget() ||
            isUnresolvedProgress() ||
            (isMeaningfulProgress() && !isWatchedProgress())
