package su.afk.yummy.tv.core.model.anime

private const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L
private const val WATCHED_REMAINING_MS = 5 * 60 * 1000L
private const val SHORT_EPISODE_WATCHED_PROGRESS = 0.90f

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

fun AnimeWatchProgress.progress(): Float =
    if (durationMs > 0) {
        (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

fun AnimeWatchProgress.isMeaningfulProgress(): Boolean =
    durationMs > 0 && positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS

fun AnimeWatchProgress.isContinueTarget(): Boolean =
    positionMs == 0L &&
            durationMs == 0L &&
            episode.isNotBlank() &&
            episodeUrl.isNotBlank()

fun AnimeWatchProgress.hasPlayableTarget(): Boolean =
    videoId > 0 || episode.isNotBlank() || episodeUrl.isNotBlank()

fun AnimeWatchProgress.isUnresolvedProgress(): Boolean =
    durationMs == 0L &&
            positionMs >= MIN_CONTINUE_WATCHING_POSITION_MS &&
            hasPlayableTarget()

fun AnimeWatchProgress.isWatchedProgress(): Boolean {
    if (!isMeaningfulProgress()) return false
    return if (durationMs <= WATCHED_REMAINING_MS) {
        progress() >= SHORT_EPISODE_WATCHED_PROGRESS
    } else {
        positionMs >= durationMs - WATCHED_REMAINING_MS
    }
}

fun AnimeWatchProgress.isContinueWatchingProgress(): Boolean =
    isContinueTarget() ||
            isUnresolvedProgress() ||
            (isMeaningfulProgress() && !isWatchedProgress())
