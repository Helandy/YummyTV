package su.afk.yummy.tv.domain.anime.model

private const val MIN_CONTINUE_WATCHING_POSITION_MS = 30_000L
private const val WATCHED_PROGRESS = 0.90f

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

fun AnimeWatchProgress.isWatchedProgress(): Boolean =
    isMeaningfulProgress() && progress() >= WATCHED_PROGRESS

fun AnimeWatchProgress.isContinueWatchingProgress(): Boolean =
    isContinueTarget() ||
            isUnresolvedProgress() ||
            (isMeaningfulProgress() && !isWatchedProgress())
