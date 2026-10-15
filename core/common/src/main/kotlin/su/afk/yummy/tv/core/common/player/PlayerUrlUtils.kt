package su.afk.yummy.tv.core.common.player

fun String.isKodikPlayerUrl(): Boolean =
    contains("kodik", ignoreCase = true)

fun String.isAksorPlayerUrl(): Boolean =
    contains("aksor.tv", ignoreCase = true)

fun String.isCvhPlayerUrl(): Boolean =
    contains("iframecvh", ignoreCase = true) ||
        equals("cvh", ignoreCase = true)

fun String.isAllohaPlayerUrl(): Boolean =
    contains("alloha", ignoreCase = true)

fun String.isVkPlayerUrl(): Boolean =
    contains("vk.com", ignoreCase = true) ||
        contains("vkvideo", ignoreCase = true) ||
        contains("video_ext.php", ignoreCase = true) ||
        contains("iframevk", ignoreCase = true)

fun String.isRutubePlayerUrl(): Boolean =
    contains("rutube.ru", ignoreCase = true)

fun String.isSibnetPlayerUrl(): Boolean =
    contains("sibnet.ru", ignoreCase = true)

fun String.isZedfilmPlayerUrl(): Boolean =
    contains("zedfilm.ru", ignoreCase = true) ||
        contains("hlamer.ru", ignoreCase = true)

/** Схема источников, подключаемых отдельными модулями (`source://<id>/…`): ссылка на файл появляется только при разрешении. */
fun String.isExtraSourceUrl(): Boolean =
    startsWith("source://", ignoreCase = true)

fun String.isSupportedPlayerUrl(): Boolean =
    isExtraSourceUrl() ||
        isKodikPlayerUrl() ||
        isAksorPlayerUrl() ||
        isCvhPlayerUrl() ||
        isAllohaPlayerUrl() ||
        isVkPlayerUrl() ||
        isRutubePlayerUrl() ||
        isSibnetPlayerUrl() ||
        isZedfilmPlayerUrl()

fun String.playerDisplayOrderPriority(): Int =
    when {
        isExtraSourceUrl() -> -1
        isCvhPlayerUrl() -> 0
        isKodikPlayerUrl() -> 1
        else -> 2
    }
