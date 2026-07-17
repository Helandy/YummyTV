package su.afk.yummy.tv.core.utils

import java.net.URI

fun String.toHttpsUrl(): String = when {
    startsWith("//") -> "https:$this"
    startsWith("http://") -> replaceFirst("http://", "https://")
    else -> this
}

fun String.normalizedHttpUrl(): String =
    when {
        startsWith("//") -> "https:$this"
        startsWith("http://") || startsWith("https://") -> this
        isNotBlank() -> "https://$this"
        else -> this
    }

fun String.httpOriginOrNull(): String? =
    runCatching {
        val uri = URI(this)
        val scheme = uri.scheme ?: return@runCatching null
        val host = uri.host ?: return@runCatching null
        val port = uri.port.takeIf { it > 0 }?.let { ":$it" }.orEmpty()
        "$scheme://$host$port"
    }.getOrNull()
