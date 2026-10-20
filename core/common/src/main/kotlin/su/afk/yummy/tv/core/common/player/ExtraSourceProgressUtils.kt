package su.afk.yummy.tv.core.common.player

private const val MIRROR_POSITION_TOLERANCE_MS = 10_000L

/**
 * Серверный прогресс — «зеркало» локального прогресса источника-расширения (4K).
 *
 * Сервер принимает прогресс 4K под настоящим видео другого балансера (Kodik, Aksor, CVH), и его дата
 * новее локальной. Зеркалом считаем запись, не ушедшую вперёд по позиции: тогда локальная запись
 * остаётся главной и «Продолжить» открывает 4K, а не балансер-двойник. Озвучки не сравниваем: у 4K они
 * свои, и сервер получает прогресс под видео с любой подходящей озвучкой. Если сервер продвинулся
 * дальше (смотрели на другом устройстве), запись не зеркало и сервер побеждает.
 */
fun isServerMirrorOfExtraSource(
    localEpisodeUrl: String,
    localPositionMs: Long,
    serverEpisodeUrl: String,
    serverPositionMs: Long,
): Boolean =
    localEpisodeUrl.isExtraSourceUrl() &&
        !serverEpisodeUrl.isExtraSourceUrl() &&
        serverPositionMs <= localPositionMs + MIRROR_POSITION_TOLERANCE_MS
