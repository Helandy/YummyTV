package su.afk.yummy.tv.android.appfunctions.model

import androidx.appfunctions.AppFunctionSerializable

/** One day in the weekly anime release schedule. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class AnimeScheduleDayResult(
    /** Day label supplied by the catalog. */
    val day: String,
    /** Anime scheduled under this day. */
    val entries: List<AnimeScheduleEntry>,
)
