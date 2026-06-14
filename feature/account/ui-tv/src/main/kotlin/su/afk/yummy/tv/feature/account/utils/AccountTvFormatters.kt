package su.afk.yummy.tv.feature.account.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserSocialCounts
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.UserWatchTypeStat
import su.afk.yummy.tv.feature.account.R
import su.afk.yummy.tv.feature.account.model.ProfileWatchSlice
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

internal fun UserProfileSummary.watchSlices(): List<ProfileWatchSlice> =
    watchTypes
        .filter { it.spentSeconds > 0L }
        .sortedByDescending { it.spentSeconds }
        .mapIndexed { index, item ->
            ProfileWatchSlice(
                title = item.title.ifBlank { item.shortName.ifBlank { item.alias } },
                shortName = item.shortName.ifBlank { item.title.ifBlank { item.alias } },
                seconds = item.spentSeconds,
                color = item.profileColor(index),
            )
        }

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

private val fallbackProfileColors = listOf(
    Color(0xFF64B5F6),
    Color(0xFFFFD166),
    Color(0xFF4DD0E1),
    Color(0xFF81C784),
)

private const val SECONDS_IN_HOUR = 3600L
