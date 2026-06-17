package su.afk.yummy.tv.domain.library.model

const val FAVORITE_ONLY_LIBRARY_LIST_ID = -1

data class LibraryItem(
    val animeId: Int,
    val title: String,
    val poster: LibraryPoster? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val listId: Int = 0,
    val isFavorite: Boolean = false,
    val listUpdatedAt: Long = addedAt,
    val favoriteUpdatedAt: Long = if (isFavorite) addedAt else 0L,
    val userRating: Int? = null,
)
