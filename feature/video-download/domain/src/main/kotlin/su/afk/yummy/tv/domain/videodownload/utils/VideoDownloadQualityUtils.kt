package su.afk.yummy.tv.domain.videodownload.utils

private val VIDEO_QUALITY_REGEX = Regex("""\d{3,4}p?""", RegexOption.IGNORE_CASE)

internal fun String.hasVideoQualityNumber(): Boolean =
    VIDEO_QUALITY_REGEX.containsMatchIn(this)
