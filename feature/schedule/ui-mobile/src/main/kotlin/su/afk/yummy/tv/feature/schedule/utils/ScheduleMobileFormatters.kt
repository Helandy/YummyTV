package su.afk.yummy.tv.feature.schedule.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal fun Long.formatAirTime(): String =
    DateTimeFormatter.ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.ofEpochSecond(this))
