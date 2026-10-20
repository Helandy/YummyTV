package su.afk.yummy.tv.data.account.repository

internal const val ACCOUNT_SHORT_TTL_MS = 5 * 60 * 1000L

/**
 * Уведомления и их счётчики: прочитанное на другом устройстве не должно висеть непрочитанным.
 * Строго меньше пятиминутного интервала опроса в MainViewModel — иначе очередной тик приходится
 * ровно на границу TTL и отдаёт кэш вместо запроса.
 */
internal const val ACCOUNT_NOTIFICATIONS_TTL_MS = 2 * 60 * 1000L

/** Список друзей несёт время последнего визита: дольше кэшировать — показывать вышедшего друга онлайн. */
internal const val ACCOUNT_FRIENDS_TTL_MS = 15 * 60 * 1000L
internal const val ACCOUNT_MEDIUM_TTL_MS = 30 * 60 * 1000L
internal const val ACCOUNT_LONG_TTL_MS = 6 * 60 * 60 * 1000L
internal const val ANIME_LIST_STATE_TTL_MS = 24 * 60 * 60 * 1000L
internal const val ACCOUNT_PAGE_CACHE_RETENTION_MS = 24 * 60 * 60 * 1000L
