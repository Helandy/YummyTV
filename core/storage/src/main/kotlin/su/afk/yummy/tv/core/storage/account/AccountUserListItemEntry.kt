package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_user_list_items",
    primaryKeys = ["userId", "listId", "language", "position"],
    indices = [
        Index(
            value = ["userId", "listId", "language"],
            name = "index_account_user_list_items_page",
        ),
    ],
)
data class AccountUserListItemEntry(
    val userId: Int,
    val listId: Int,
    val language: String,
    val position: Int,
    val animeId: Int,
    val title: String,
    val posterUrl: String? = null,
    val posterSmallUrl: String? = null,
    val posterMediumUrl: String? = null,
    val posterBigUrl: String? = null,
    val posterFullsizeUrl: String? = null,
    val posterMegaUrl: String? = null,
    val rating: Double? = null,
    val userRating: Int? = null,
    val year: Int? = null,
    val userListId: Int? = null,
    val isFavorite: Boolean,
    val updatedAtSeconds: Long? = null,
    val nextEpisodeAtSeconds: Long? = null,
    /** Сезон выхода, слаг [su.afk.yummy.tv.core.model.anime.AnimeSeason]. */
    val season: String? = null,
)
