package su.afk.yummy.tv.domain.player

fun String.isKodikPlayerUrl(): Boolean =
    contains("kodik", ignoreCase = true)

fun String.isAksorPlayerUrl(): Boolean =
    contains("aksor.tv", ignoreCase = true)

fun String.isCvhPlayerUrl(): Boolean =
    contains("iframecvh", ignoreCase = true)

fun String.isAllohaPlayerUrl(): Boolean =
    contains("alloha", ignoreCase = true)

fun String.isVkPlayerUrl(): Boolean =
    contains("vk.com", ignoreCase = true) ||
            contains("vkvideo", ignoreCase = true) ||
            contains("video_ext.php", ignoreCase = true) ||
            contains("iframevk", ignoreCase = true)

fun String.isRutubePlayerUrl(): Boolean =
    contains("rutube.ru", ignoreCase = true)

fun String.isSupportedPlayerUrl(): Boolean =
    isKodikPlayerUrl() ||
            isAksorPlayerUrl() ||
            isCvhPlayerUrl() ||
            isAllohaPlayerUrl() ||
            isVkPlayerUrl() ||
            isRutubePlayerUrl()
