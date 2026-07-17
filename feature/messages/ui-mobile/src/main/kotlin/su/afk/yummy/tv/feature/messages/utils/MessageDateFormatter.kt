package su.afk.yummy.tv.feature.messages.mobile.utils

import android.content.Context
import android.text.format.DateUtils

internal fun Long.formatMessageDate(context: Context): String =
    DateUtils.getRelativeDateTimeString(
        context,
        this * 1_000L,
        DateUtils.MINUTE_IN_MILLIS,
        DateUtils.WEEK_IN_MILLIS,
        DateUtils.FORMAT_ABBREV_RELATIVE,
    ).toString()
