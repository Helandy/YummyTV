package su.afk.yummy.tv.feature.library.utils

import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.LibraryTab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun LibraryItem.tvDateText(tab: LibraryTab): String? =
    when (tab) {
        LibraryTab.FAVORITES -> favoriteUpdatedAt
        LibraryTab.CONTINUE_WATCHING -> 0L
        else -> listUpdatedAt
    }.formatLibraryDate()

internal fun LibraryItem.tvUserRating(): Double? =
    userRating?.takeIf { it in 1..10 }?.toDouble()

private fun Long.formatLibraryDate(): String? =
    takeIf { it > 0L }?.let {
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(Date(it))
    }
