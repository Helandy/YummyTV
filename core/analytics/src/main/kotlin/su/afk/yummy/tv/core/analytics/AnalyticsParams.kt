package su.afk.yummy.tv.core.analytics

/**
 * Builds a normalized analytics params map.
 *
 * Null values and blank strings are skipped. Non-string values are converted with [Any.toString].
 */
fun analyticsParamsOf(vararg params: Pair<String, Any?>): Map<String, String> =
    params.mapNotNull { (key, value) ->
        val normalized = when (value) {
            null -> return@mapNotNull null
            is String -> value.takeIf { it.isNotBlank() }
            else -> value.toString()
        } ?: return@mapNotNull null
        key to normalized
    }.toMap()
