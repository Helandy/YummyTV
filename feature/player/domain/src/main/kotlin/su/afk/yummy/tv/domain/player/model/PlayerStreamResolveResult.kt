package su.afk.yummy.tv.domain.player.model

sealed interface PlayerStreamResolveResult {
    data class Stream(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val qualities: LinkedHashMap<String, String>? = null,
        val qualityHeaders: Map<String, Map<String, String>> = emptyMap(),
        val allohaAudioTracks: List<AllohaAudioTrack> = emptyList(),
        val selectedAllohaAudioId: String? = null,
        val allohaSubtitles: List<AllohaSubtitleTrack> = emptyList(),
        /**
         * Резервный узел CDN для этих же подписанных ссылок: подпись от узла не зависит, поэтому
         * при отказе узла можно переехать подменой хоста, не перезапрашивая ссылку у балансера.
         */
        val failoverHost: String? = null,
    ) : PlayerStreamResolveResult

    data class KodikBlocked(
        val message: String? = null,
        val statusCode: Int? = null,
    ) : PlayerStreamResolveResult

    /** The source resolved successfully but reports this specific dubbing/episode has no stream. */
    data class Unavailable(
        val message: String? = null,
        val cause: PlayerStreamUnavailableCause? = null,
    ) : PlayerStreamResolveResult

    /**
     * Источник не отдал поток. [reason] — короткая техническая причина (шаг экстрактора, HTTP-код)
     * для диагностики; пользователю не показывается.
     */
    data class Failed(val reason: String? = null) : PlayerStreamResolveResult

    data object Unsupported : PlayerStreamResolveResult
}
