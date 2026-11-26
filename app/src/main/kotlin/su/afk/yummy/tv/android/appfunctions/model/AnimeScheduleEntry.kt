package su.afk.yummy.tv.android.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/** An anime listed in the weekly release schedule. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AnimeScheduleEntry(
    /** YummyTV catalog ID. */
    val animeId: Int,
    /** Display title of the anime. */
    val title: String,
    /** Next scheduled episode time in Unix seconds, when available. */
    val nextEpisodeAt: Long?,
    /** Number of released episodes when known. */
    val airedEpisodes: Int?,
    /** Planned episode count when known. */
    val totalEpisodes: Int?,
)
