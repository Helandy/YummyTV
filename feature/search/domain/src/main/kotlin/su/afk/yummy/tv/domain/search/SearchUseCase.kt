package su.afk.yummy.tv.domain.search

class SearchUseCase(private val repository: SearchRepository) {
    suspend operator fun invoke(query: String, limit: Int = 40, offset: Int = 0): List<SearchItem> =
        repository.search(query, limit, offset)
}
