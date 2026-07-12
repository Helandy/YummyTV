package su.afk.yummy.tv.domain.player.model

sealed interface PlayerStreamResolveResult {
    data class Stream(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val qualities: LinkedHashMap<String, String>? = null,
        val qualityHeaders: Map<String, Map<String, String>> = emptyMap(),
    ) : PlayerStreamResolveResult

    data class KodikBlocked(
        val message: String? = null,
        val statusCode: Int? = null,
    ) : PlayerStreamResolveResult

    /** The source resolved successfully but reports this specific dubbing/episode has no stream. */
    data class Unavailable(
        val message: String? = null,
    ) : PlayerStreamResolveResult

    data object Failed : PlayerStreamResolveResult
    data object Unsupported : PlayerStreamResolveResult
}
