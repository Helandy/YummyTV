package su.afk.yummy.tv.domain.search.model

data class SearchFilterOptions(
    val genreGroups: List<SearchGenreGroup> = emptyList(),
    val genres: List<SearchGenre> = emptyList(),
    val types: List<SearchAnimeType> = emptyList(),
)
