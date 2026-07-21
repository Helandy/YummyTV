package su.afk.yummy.tv.feature.account.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserListWatchStat
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserSocialCounts
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.UserWatchTypeStat
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.account.model.ProfileStatSlice
import su.afk.yummy.tv.feature.account.account.model.ProfileWatchSlice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun UserStats.isEmpty(): Boolean =
    genres.isEmpty() && ratings.isEmpty() && lists.isEmpty() && types.isEmpty()

internal fun Long.toDurationLabel(): String {
    val hours = this / 3600L
    val minutes = (this % 3600L) / 60L
    val seconds = this % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

internal fun Long.formatDate(): String =
    if (this <= 0L) "" else SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(
        Date(
            this * 1000L
        )
    )

internal fun Long.formatProfileDate(): String =
    if (this <= 0L) "" else SimpleDateFormat(
        "dd.MM.yy",
        Locale.getDefault()
    ).format(Date(this * 1000L))

@Composable
internal fun Long.toProfileHoursLabel(): String {
    val hours = this / SECONDS_IN_HOUR
    return if (hours > 0L) {
        stringResource(R.string.account_profile_duration_hours, hours)
    } else {
        stringResource(R.string.account_profile_duration_less_hour)
    }
}

@Composable
internal fun UserProfileSex.label(): String =
    stringResource(
        when (this) {
            UserProfileSex.MALE -> R.string.account_profile_sex_male
            UserProfileSex.FEMALE -> R.string.account_profile_sex_female
            UserProfileSex.UNKNOWN -> R.string.account_profile_unknown
        }
    )

internal fun UserProfileSummary.totalWatchSeconds(): Long =
    watchTypes.sumOf { it.spentSeconds }.coerceAtLeast(0L)

@Composable
internal fun UserProfileSummary.watchSlices(): List<ProfileWatchSlice> {
    val movieLabel = stringResource(R.string.account_profile_watch_type_movies)
    return watchTypes
        .filter { it.spentSeconds > 0L }
        .sortedByDescending { it.spentSeconds }
        .mapIndexed { index, item ->
            ProfileWatchSlice(
                title = item.title.ifBlank { item.shortName.ifBlank { item.alias } },
                shortName = item.profileShortName(movieLabel),
                seconds = item.spentSeconds,
                color = item.profileColor(index),
            )
        }
}

@Composable
internal fun UserProfileSummary.watchStatSlices(): List<ProfileStatSlice> =
    watchSlices().map { slice ->
        ProfileStatSlice(
            title = slice.shortName,
            value = slice.seconds,
            color = slice.color,
        )
    }

internal fun UserStats.listDurationSlices(): List<ProfileStatSlice> =
    lists
        .filter { it.seconds > 0L }
        .sortedByDescending { it.seconds }
        .mapIndexed { index, item ->
            ProfileStatSlice(
                title = item.title,
                value = item.seconds,
                color = item.listProfileColor(index),
            )
        }

internal fun UserStats.genreCountSlices(): List<ProfileStatSlice> =
    genres
        .filter { it.count > 0 }
        .sortedByDescending { it.count }
        .take(MAX_PROFILE_GENRE_SLICES)
        .mapIndexed { index, item ->
            ProfileStatSlice(
                title = item.title,
                value = item.count.toLong(),
                color = fallbackProfileColors[index % fallbackProfileColors.size],
            )
        }

internal fun UserStats.ratingCountSlices(): List<ProfileStatSlice> {
    val byRating = ratings.associateBy { it.rating }
    return (10 downTo 1).map { rating ->
        ProfileStatSlice(
            title = rating.toString(),
            value = (byRating[rating]?.count ?: 0).toLong(),
            color = rating.profileRatingColor(),
        )
    }
}

internal fun UserStats.averageRatingLabel(): String {
    val totalCount = ratings.sumOf { it.count }
    if (totalCount <= 0) return "0"
    val weightedSum = ratings.sumOf { it.rating * it.count }
    val average = weightedSum.toDouble() / totalCount.toDouble()
    return if (average % 1.0 == 0.0) {
        average.toInt().toString()
    } else {
        String.format(Locale.getDefault(), "%.1f", average)
    }
}

internal fun List<ProfileStatSlice>.positiveValueSum(): Long =
    sumOf { it.value.coerceAtLeast(0L) }

internal fun UserProfileCounts.totalLibraryCount(): Int =
    watching + planned + completed + dropped + postponed

internal fun UserSocialCounts.hasAny(): Boolean =
    friends + reviews + comments + posts + collections > 0

private fun UserWatchTypeStat.profileColor(index: Int): Color =
    when (alias) {
        "tv" -> Color(0xFFA678E8)
        "movie" -> Color(0xFFC84BD6)
        "ona" -> Color(0xFFFF9E34)
        "special" -> Color(0xFF8F939A)
        "ova" -> Color(0xFFFF6B75)
        "short-tv" -> Color(0xFFFF4D4D)
        "short-movie" -> Color(0xFF63D2B6)
        else -> fallbackProfileColors[index % fallbackProfileColors.size]
    }

private fun UserWatchTypeStat.profileShortName(movieLabel: String): String =
    when (alias) {
        "movie" -> movieLabel
        else -> shortName.ifBlank { title.ifBlank { alias } }
    }

private fun UserListWatchStat.listProfileColor(index: Int): Color =
    when (id) {
        0 -> Color(0xFFFF6B6B)
        1 -> Color(0xFFA678E8)
        2 -> Color(0xFF69D38B)
        3 -> Color(0xFF9CA3AF)
        4 -> Color(0xFFFFC857)
        5 -> Color(0xFFD86BFF)
        else -> fallbackProfileColors[index % fallbackProfileColors.size]
    }

private fun Int.profileRatingColor(): Color =
    when (this) {
        10, 9 -> Color(0xFF72C557)
        8, 7 -> Color(0xFF8BC34A)
        6, 5 -> Color(0xFFFFD234)
        4 -> Color(0xFFFFB300)
        3, 2 -> Color(0xFFFF7A5C)
        1 -> Color(0xFFFF5C6C)
        else -> Color(0xFF8F939A)
    }

private val fallbackProfileColors = listOf(
    Color(0xFF45D487),
    Color(0xFFA678E8),
    Color(0xFFC84BD6),
    Color(0xFFFFC107),
    Color(0xFFFF6B6B),
    Color(0xFF8F939A),
    Color(0xFF64B5F6),
    Color(0xFF4DD0E1),
)

private const val MAX_PROFILE_GENRE_SLICES = 12
private const val SECONDS_IN_HOUR = 3600L
