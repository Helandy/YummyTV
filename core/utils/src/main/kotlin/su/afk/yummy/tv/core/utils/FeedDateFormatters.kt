package su.afk.yummy.tv.core.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Единый формат даты для лент (рецензии, посты, видео блогеров): «день месяц, часы:минуты».
 * Вход — Unix-время в секундах. Единый источник, чтобы не плодить inline-[java.text.DateFormat] по фичам.
 */
fun Long.formatFeedDateTime(): String =
    SimpleDateFormat("d MMMM, HH:mm", Locale.getDefault()).format(Date(this * 1_000L))
