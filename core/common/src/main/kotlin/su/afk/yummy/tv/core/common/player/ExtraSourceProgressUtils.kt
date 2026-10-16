package su.afk.yummy.tv.core.common.player

private const val MIRROR_POSITION_TOLERANCE_MS = 10_000L

/**
 * Серверный прогресс — «зеркало» локального прогресса источника-расширения (4K).
 *
 * Сервер принимает прогресс 4K под настоящим видео другого балансера (Aksor, CVH), и его дата новее
 * локальной. Зеркалом считаем запись той же озвучки, не ушедшую вперёд по позиции: тогда локальная
 * запись остаётся главной и «Продолжить» открывает 4K, а не балансер-двойник. Если сервер продвинулся
 * дальше (смотрели на другом устройстве) или озвучка другая, запись не зеркало и сервер побеждает.
 */
fun isServerMirrorOfExtraSource(
    localEpisodeUrl: String,
    localDubbing: String,
    localPositionMs: Long,
    serverEpisodeUrl: String,
    serverDubbing: String,
    serverPositionMs: Long,
): Boolean =
    localEpisodeUrl.isExtraSourceUrl() &&
        !serverEpisodeUrl.isExtraSourceUrl() &&
        localDubbing.trim() == serverDubbing.trim() &&
        serverPositionMs <= localPositionMs + MIRROR_POSITION_TOLERANCE_MS
