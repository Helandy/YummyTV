package su.afk.yummy.tv.core.storage.account

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "account_anime_list_states",
    primaryKeys = ["userId", "animeId"],
    indices = [
        Index(value = ["cachedAt"], name = "index_account_anime_list_states_cachedAt"),
    ],
)
data class AccountAnimeListStateEntry(
    val userId: Int,
    val animeId: Int,
    val listId: Int? = null,
    val isFavorite: Boolean,
    val cachedAt: Long,
)
