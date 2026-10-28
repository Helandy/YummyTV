package su.afk.yummy.tv.source.availability.domain.model

/**
 * Вариант озвучки у источника: [key] (id релиза, id озвучки в эмбеде) нужен, чтобы запросить поток серии; [isDubbing] — «новый
 * дубляж» рядом с озвучкой; [title] — название озвучки, если источник отдаёт озвучки по именам (AniBoom); [isSubs] — вместо озвучки субтитры (Mikai).
 */
data class SourceVariant(
    val key: String,
    val isDubbing: Boolean,
    val episodes: List<SourceEpisode>,
    val title: String? = null,
    val isSubs: Boolean = false,
)
