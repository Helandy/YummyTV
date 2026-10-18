package su.afk.yummy.tv.feature.account.common.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import java.util.Locale
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.core.utils.formatting.formatLocalizedDateSeconds
import su.afk.yummy.tv.core.utils.formatting.formatLocalizedDateTimeSeconds
import su.afk.yummy.tv.core.utils.formatting.formatLocalizedShortDateSeconds
import su.afk.yummy.tv.core.utils.formatting.formatRelativeDateTime
import su.afk.yummy.tv.domain.account.model.UserListWatchStat
import su.afk.yummy.tv.domain.account.model.UserProfileCounts
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserSocialCounts
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.UserWatchTypeStat
import su.afk.yummy.tv.domain.account.model.ratingsByValue
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice
import su.afk.yummy.tv.feature.account.common.model.ProfileWatchSlice
import su.afk.yummy.tv.feature.account.presentation.R

fun UserStats.isEmpty(): Boolean =
    genres.isEmpty() && ratings.isEmpty() && lists.isEmpty() && types.isEmpty()

fun Long.toDurationLabel(): String {
    val hours = this / 3600L
    val minutes = (this % 3600L) / 60L
    val seconds = this % 60L
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m"
        else -> "${seconds}s"
    }
}

fun Long.formatDate(): String =
    if (this <= 0L) "" else formatLocalizedDateTimeSeconds()

fun Long.formatProfileDate(): String =
    if (this <= 0L) "" else formatLocalizedShortDateSeconds()

@Composable
fun Long.toProfileHoursLabel(): String {
    val hours = this / SECONDS_IN_HOUR
    return if (hours > 0L) {
        stringResource(R.string.account_profile_duration_hours, hours)
    } else {
        stringResource(R.string.account_profile_duration_less_hour)
    }
}

@Composable
fun UserProfileSex.label(): String =
    stringResource(
        when (this) {
            UserProfileSex.MALE -> R.string.account_profile_sex_male
            UserProfileSex.FEMALE -> R.string.account_profile_sex_female
            UserProfileSex.UNKNOWN -> R.string.account_profile_unknown
        }
    )

fun UserProfileSummary.totalWatchSeconds(): Long =
    watchTypes.sumOf { it.spentSeconds }.coerceAtLeast(0L)

@Composable
fun UserProfileSummary.watchSlices(): List<ProfileWatchSlice> {
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

fun UserStats.averageRatingLabel(): String {
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

/** «950», «1.2k», «12k» — подпись над столбцом гистограммы. */
fun Long.toCompactCountLabel(): String =
    when {
        this >= 10_000L -> "${this / 1_000L}k"
        this >= 1_000L -> String.format(Locale.US, "%.1fk", this / 1_000.0)
        else -> toString()
    }

fun List<ProfileStatSlice>.positiveValueSum(): Long =
    sumOf { it.value.coerceAtLeast(0L) }

fun UserSocialCounts.hasAny(): Boolean =
    friends + reviews + comments + posts + collections > 0

fun UserWatchTypeStat.profileColor(index: Int): Color =
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

fun UserWatchTypeStat.profileShortName(movieLabel: String): String =
    when (alias) {
        "movie" -> movieLabel
        else -> shortName.ifBlank { title.ifBlank { alias } }
    }

fun UserListWatchStat.listProfileColor(index: Int): Color =
    when (id) {
        0 -> YummySemanticColors.StatusWatching
        1 -> YummySemanticColors.StatusPlanned
        2 -> YummySemanticColors.StatusCompleted
        3 -> YummySemanticColors.StatusDropped
        4 -> YummySemanticColors.StatusPostponed
        5 -> YummySemanticColors.StatusFavorite
        else -> fallbackProfileColors[index % fallbackProfileColors.size]
    }

fun Int.profileRatingColor(): Color =
    when (this) {
        10, 9 -> Color(0xFF72C557)
        8, 7 -> Color(0xFF8BC34A)
        6, 5 -> Color(0xFFFFD234)
        4 -> Color(0xFFFFB300)
        3, 2 -> Color(0xFFFF7A5C)
        1 -> Color(0xFFFF5C6C)
        else -> Color(0xFF8F939A)
    }

val fallbackProfileColors = listOf(
    Color(0xFF45D487),
    Color(0xFFA678E8),
    Color(0xFFC84BD6),
    Color(0xFFFFC107),
    Color(0xFFFF6B6B),
    Color(0xFF8F939A),
    Color(0xFF64B5F6),
    Color(0xFF4DD0E1),
)

const val MAX_PROFILE_GENRE_SLICES = 12
const val SECONDS_IN_HOUR = 3600L

/**
 * Сайт держит статус живым через сокет и считает «в сети» последнюю минуту; у нас профиль и
 * друзья приходят из кэша и обновляются реже, поэтому окно шире.
 */
private const val ONLINE_WINDOW_SECONDS = 5 * 60L

fun Long.isOnlineAt(nowEpochSeconds: Long): Boolean =
    this > 0L && nowEpochSeconds - this < ONLINE_WINDOW_SECONDS

/** «В сети» или «Был(а) в сети 5 мин. назад» с учётом пола; null, если время визита неизвестно. */
@Composable
fun Long.lastOnlineLabel(sex: UserProfileSex, nowEpochSeconds: Long): String? = when {
    this <= 0L -> null
    isOnlineAt(nowEpochSeconds) -> stringResource(R.string.user_profile_online)
    else -> stringResource(
        when (sex) {
            UserProfileSex.MALE -> R.string.user_profile_last_seen_male
            UserProfileSex.FEMALE -> R.string.user_profile_last_seen_female
            UserProfileSex.UNKNOWN -> R.string.user_profile_last_seen_unknown
        },
        formatRelativeDateTime(nowEpochSeconds * 1_000L),
    )
}
