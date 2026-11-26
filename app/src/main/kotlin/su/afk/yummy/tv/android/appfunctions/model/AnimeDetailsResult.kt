package su.afk.yummy.tv.android.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/** Public catalog details for an anime. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AnimeDetailsResult(
    /** YummyTV catalog ID. */
    val animeId: Int,
    /** Display title of the anime. */
    val title: String,
    /** Synopsis, or an empty string if unavailable. */
    val description: String,
    /** Release year when known. */
    val year: Int?,
    /** Release status as reported by the catalog. */
    val status: String?,
    /** Number of released episodes when known. */
    val airedEpisodes: Int?,
    /** Planned episode count when known. */
    val totalEpisodes: Int?,
)
