package su.afk.yummy.tv.core.utils.formatting

import android.text.format.DateUtils

fun Long.formatRelativeDateTime(nowMillis: Long): String =
    DateUtils.getRelativeTimeSpanString(
        this * 1_000L,
        nowMillis,
        DateUtils.MINUTE_IN_MILLIS,
    ).toString()
