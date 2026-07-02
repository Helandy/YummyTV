package su.afk.yummy.tv.core.utils

fun Map<String, String>.safeHttpHeaderNames(): List<String> =
    keys.map { it.lowercase() }
        .filterNot { it == "cookie" || it == "authorization" }
        .sorted()
