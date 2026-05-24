package su.afk.yummy.tv.domain.top100

interface AnimeTopRepository {
    suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): AnimeTopPage
}

data class AnimeTopPage(
    val items: List<AnimeTopItem>,
    val nextOffset: Int,
    val canLoadMore: Boolean,
)
