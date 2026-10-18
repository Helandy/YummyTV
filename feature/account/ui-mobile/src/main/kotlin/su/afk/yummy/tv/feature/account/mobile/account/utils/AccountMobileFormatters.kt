package su.afk.yummy.tv.feature.account.mobile.account.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.toPersistentList
import su.afk.yummy.tv.core.designsystem.theme.YummySemanticColors
import su.afk.yummy.tv.domain.account.model.UserListWatchStat
import su.afk.yummy.tv.domain.account.model.UserProfileSex
import su.afk.yummy.tv.domain.account.model.UserProfileSummary
import su.afk.yummy.tv.domain.account.model.UserStats
import su.afk.yummy.tv.domain.account.model.UserWatchTypeStat
import su.afk.yummy.tv.domain.account.model.ratingsByValue
import su.afk.yummy.tv.feature.account.presentation.R
import java.util.Locale
import su.afk.yummy.tv.core.utils.formatting.formatLocalizedDateTimeSeconds
import su.afk.yummy.tv.core.utils.formatting.formatLocalizedShortDateSeconds
import su.afk.yummy.tv.core.utils.formatting.formatLocalizedDateSeconds
import su.afk.yummy.tv.feature.account.common.utils.MAX_PROFILE_GENRE_SLICES
import su.afk.yummy.tv.feature.account.common.utils.fallbackProfileColors
import su.afk.yummy.tv.feature.account.common.utils.listProfileColor
import su.afk.yummy.tv.feature.account.common.utils.profileRatingColor
import su.afk.yummy.tv.feature.account.common.utils.watchSlices
import su.afk.yummy.tv.feature.account.common.model.ProfileStatSlice

internal fun Long.formatUserListDate(): String =
    if (this <= 0L) "" else formatLocalizedDateSeconds()

@Composable
internal fun UserProfileSummary.watchStatSlices(): PersistentList<ProfileStatSlice> =
    watchSlices().map { slice ->
        ProfileStatSlice(
            title = slice.shortName,
            value = slice.seconds,
            color = slice.color,
        )
    }.toPersistentList()

internal fun UserStats.listDurationSlices(): PersistentList<ProfileStatSlice> =
    lists
        .filter { it.seconds > 0L }
        .sortedByDescending { it.seconds }
        .mapIndexed { index, item ->
            ProfileStatSlice(
                title = item.title,
                value = item.seconds,
                color = item.listProfileColor(index),
            )
        }.toPersistentList()

internal fun UserStats.genreCountSlices(): PersistentList<ProfileStatSlice> =
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
        }.toPersistentList()

internal fun UserStats.ratingCountSlices(): PersistentList<ProfileStatSlice> {
    val byRating = ratingsByValue
    return (10 downTo 1).map { rating ->
        ProfileStatSlice(
            title = rating.toString(),
            value = (byRating[rating]?.count ?: 0).toLong(),
            color = rating.profileRatingColor(),
        )
    }.toPersistentList()
}
