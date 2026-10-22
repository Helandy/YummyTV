package su.afk.yummy.tv.domain.player.utils

private const val HEIGHT_CAP_PREFIX = "yummy-max-height="

/**
 * Ссылка на тот же HLS-мастер, но с ограничением высоты видео: качество кодируется во фрагменте
 * (`master.m3u8#yummy-max-height=720`), HTTP-запрос от него не меняется, а плеер ограничивает выбор варианта.
 * Нужно там, где звук лежит отдельной группой и ссылка на одиночный вариант осталась бы без него.
 */
fun String.withMaxVideoHeight(height: Int): String = "${substringBefore('#')}#$HEIGHT_CAP_PREFIX$height"

/** Ограничение высоты, зашитое [withMaxVideoHeight]; `null`, если в ссылке его нет. */
fun String.maxVideoHeightOrNull(): Int? =
    substringAfter('#', "").takeIf { it.startsWith(HEIGHT_CAP_PREFIX) }
        ?.removePrefix(HEIGHT_CAP_PREFIX)?.toIntOrNull()?.takeIf { it > 0 }
