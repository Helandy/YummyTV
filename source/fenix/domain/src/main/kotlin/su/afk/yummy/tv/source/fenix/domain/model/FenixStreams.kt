package su.afk.yummy.tv.source.fenix.domain.model

/**
 * Поток серии на Fenix: [masterUrl] — HLS-мастер выбранной озвучки с уже подписанным доступом (токен в query). Звук у Fenix
 * отдельной дорожкой мастера, поэтому отдельных потоков по качеству нет: плеер играет мастер адаптивно.
 */
data class FenixStreams(
    val masterUrl: String,
)
