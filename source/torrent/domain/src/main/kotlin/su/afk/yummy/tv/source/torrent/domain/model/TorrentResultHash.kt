package su.afk.yummy.tv.source.torrent.domain.model

private val BTIH_REGEX = Regex("""urn:btih:([a-z0-9]+)""", RegexOption.IGNORE_CASE)
private const val BASE32_ALPHABET = "abcdefghijklmnopqrstuvwxyz234567"

/**
 * Инфо-хеш раздачи (40 символов hex, нижний регистр), если [TorrentResult.link] — magnet со `urn:btih:`.
 * Хеш в base32 (32 символа) приводится к hex; ссылка на `.torrent`-файл и неразборчивый хеш дают `null`.
 */
val TorrentResult.infoHash: String?
    get() {
        val raw = BTIH_REGEX.find(link)?.groupValues?.get(1)?.lowercase() ?: return null
        return when {
            raw.length == 40 && raw.all { it in '0'..'9' || it in 'a'..'f' } -> raw
            raw.length == 32 && raw.all { it in BASE32_ALPHABET } -> base32ToHex(raw)
            else -> null
        }
    }

private fun base32ToHex(value: String): String {
    var bits = 0L
    var bitCount = 0
    val hex = StringBuilder()
    for (char in value) {
        bits = (bits shl 5) or BASE32_ALPHABET.indexOf(char).toLong()
        bitCount += 5
        while (bitCount >= 8) {
            bitCount -= 8
            hex.append(((bits shr bitCount) and 0xFF).toString(16).padStart(2, '0'))
        }
    }
    return hex.toString()
}
