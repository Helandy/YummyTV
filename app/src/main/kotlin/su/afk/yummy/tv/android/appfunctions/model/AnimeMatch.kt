package su.afk.yummy.tv.android.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/** Anime matching a catalog search. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AnimeMatch(
    /** YummyTV catalog ID for details or playback. */
    val animeId: Int,
    /** Display title of the anime. */
    val title: String,
)
