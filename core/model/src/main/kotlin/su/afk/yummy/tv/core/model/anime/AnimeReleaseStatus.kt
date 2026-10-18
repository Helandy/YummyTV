package su.afk.yummy.tv.core.model.anime

/**
 * Статус выхода тайтла. [slug] совпадает с `anime_status.alias` в ответах yani и хранится в базе,
 * поэтому порядок констант менять безопасно.
 */
enum class AnimeReleaseStatus(val slug: String) {
    ONGOING("ongoing"),
    RELEASED("released"),
    ANNOUNCEMENT("announcement"),
    ;

    companion object {
        fun fromSlug(slug: String?): AnimeReleaseStatus? {
            val normalized = slug?.trim()?.lowercase() ?: return null
            return entries.firstOrNull { it.slug == normalized }
        }
    }
}
