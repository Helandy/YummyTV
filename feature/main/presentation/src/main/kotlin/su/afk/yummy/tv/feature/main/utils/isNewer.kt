package su.afk.yummy.tv.feature.main.utils

internal fun isNewer(current: String, remote: String): Boolean {
    val cur = current.split(".").map { it.toIntOrNull() ?: 0 }
    val rem = remote.split(".").map { it.toIntOrNull() ?: 0 }
    val len = maxOf(cur.size, rem.size)
    for (i in 0 until len) {
        val r = rem.getOrElse(i) { 0 }
        val c = cur.getOrElse(i) { 0 }
        if (r > c) return true
        if (r < c) return false
    }
    return false
}