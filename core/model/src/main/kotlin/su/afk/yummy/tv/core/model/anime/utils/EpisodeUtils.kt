package su.afk.yummy.tv.core.model.anime.utils

fun String.episodeNumberOrNull(): Double? {
    val normalized = trim().replace(',', '.')
    return normalized.toDoubleOrNull()
        ?: EPISODE_NUMBER_REGEX
            .find(normalized)
            ?.value
            ?.replace(',', '.')
            ?.toDoubleOrNull()
}

fun String.isPlaceholderEpisode(): Boolean = trim().isEmpty() || trim() == "-"

private val EPISODE_NUMBER_REGEX = Regex("""\d+(?:[.,]\d+)?""")
