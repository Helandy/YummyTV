package su.afk.yummy.tv.data.videodownload.worker.utils

internal fun Int.nextRetryAttempt(): Int = this + 1
