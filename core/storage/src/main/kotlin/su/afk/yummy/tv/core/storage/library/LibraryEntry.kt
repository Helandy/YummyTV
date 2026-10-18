package su.afk.yummy.tv.core.storage.library

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

const val FAVORITE_ONLY_LIST_ID = -1

@Entity(
    tableName = "library",
    indices = [
        Index(value = ["addedAt"], name = "index_library_addedAt"),
    ],
)
data class LibraryEntry(
    @PrimaryKey val animeId: Int,
    val title: String,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val listId: Int = 0,
    val isFavorite: Boolean = false,
    val listUpdatedAt: Long = addedAt,
    val favoriteUpdatedAt: Long = if (isFavorite) addedAt else 0L,
    val userRating: Int? = null,
    val year: Int? = null,
    /** Общий рейтинг тайтла, приходит вместе со списками пользователя. */
    val rating: Double? = null,
    val nextEpisodeAtSeconds: Long? = null,
    /** Сезон выхода, слаг [su.afk.yummy.tv.core.model.anime.AnimeSeason]. */
    val season: String? = null,
    /** Короткое имя типа тайтла с сервера («ТВ», «OVA», «п/ф»). */
    val kind: String? = null,
    /** Статус выхода, слаг [su.afk.yummy.tv.core.model.anime.AnimeReleaseStatus]. */
    val releaseStatus: String? = null,
)

data class LibraryPoster(
    val small: String?,
    val medium: String?,
    val big: String?,
    val fullsize: String?,
    val mega: String?,
)
