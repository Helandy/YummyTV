package su.afk.yummy.tv.domain.top100

interface AnimeTopRepository {
    suspend fun getTopAnime(type: AnimeTopType, limit: Int, offset: Int): List<AnimeTopItem>
}
