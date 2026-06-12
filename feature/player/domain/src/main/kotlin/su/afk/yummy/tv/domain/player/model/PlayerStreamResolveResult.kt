package su.afk.yummy.tv.domain.player.model

sealed interface PlayerStreamResolveResult {
    data class Stream(
        val url: String,
        val headers: Map<String, String> = emptyMap(),
        val qualities: LinkedHashMap<String, String>? = null,
    ) : PlayerStreamResolveResult

    data class KodikBlocked(
        val message: String? = null,
        val statusCode: Int? = null,
    ) : PlayerStreamResolveResult

    data object Failed : PlayerStreamResolveResult
    data object Unsupported : PlayerStreamResolveResult
}
