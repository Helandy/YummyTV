package su.afk.yummy.tv.core.utils.episode

/** Сколько осталось до выхода серии, в самой крупной подходящей единице. */
data class EpisodeReleaseCountdown(
    val value: Int,
    val unit: TimeUnit,
) {
    enum class TimeUnit {
        DAYS,
        HOURS,
        MINUTES,
    }
}

/**
 * Отсчёт до даты выхода серии ([nextDateEpochSeconds], epoch-секунды).
 * `null`, если даты нет или серия уже вышла.
 */
fun releaseCountdown(
    nextDateEpochSeconds: Long?,
    nowEpochSeconds: Long,
): EpisodeReleaseCountdown? {
    val secondsUntilRelease = (nextDateEpochSeconds ?: return null) - nowEpochSeconds
    if (secondsUntilRelease <= 0) return null

    val (value, unit) = when {
        secondsUntilRelease >= SECONDS_PER_DAY -> {
            secondsUntilRelease / SECONDS_PER_DAY to EpisodeReleaseCountdown.TimeUnit.DAYS
        }

        secondsUntilRelease >= SECONDS_PER_HOUR -> {
            secondsUntilRelease / SECONDS_PER_HOUR to EpisodeReleaseCountdown.TimeUnit.HOURS
        }

        else -> {
            (secondsUntilRelease / SECONDS_PER_MINUTE).coerceAtLeast(1) to
                    EpisodeReleaseCountdown.TimeUnit.MINUTES
        }
    }

    return EpisodeReleaseCountdown(
        value = value.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
        unit = unit,
    )
}

private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 60L * SECONDS_PER_MINUTE
private const val SECONDS_PER_DAY = 24L * SECONDS_PER_HOUR
