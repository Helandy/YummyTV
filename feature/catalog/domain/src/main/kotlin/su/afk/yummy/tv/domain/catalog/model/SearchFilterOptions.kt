package su.afk.yummy.tv.domain.catalog.model

data class SearchFilterOptions(
    val genreGroups: List<SearchGenreGroup> = emptyList(),
    val genres: List<SearchGenre> = emptyList(),
    val types: List<SearchAnimeType> = emptyList(),
)
