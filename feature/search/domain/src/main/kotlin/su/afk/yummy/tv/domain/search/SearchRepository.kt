package su.afk.yummy.tv.domain.search

interface SearchRepository {
    suspend fun search(query: String, limit: Int, offset: Int): List<SearchItem>
}
