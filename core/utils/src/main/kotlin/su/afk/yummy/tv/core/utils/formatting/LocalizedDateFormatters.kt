package su.afk.yummy.tv.core.utils.formatting

import android.text.format.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Дата и время в формате текущей локали («12.10.2026, 18:30» для ru, «10/12/2026, 6:30 PM» для en).
 * Вход — Unix-время в секундах. Форматтер создаётся на вызов: `SimpleDateFormat` небезопасен для
 * потоков, а закэшированный в `val` замораживал бы локаль на момент загрузки класса.
 */
fun Long.formatLocalizedDateTimeSeconds(): String = Date(this * 1_000L).formatBySkeleton("yMMddHHmm")

/** Дата в формате текущей локали с двузначным годом («12.10.26»). Вход — Unix-время в секундах. */
fun Long.formatLocalizedShortDateSeconds(): String = Date(this * 1_000L).formatBySkeleton("yyMMdd")

/** Дата в формате текущей локали («12.10.2026»). Вход — Unix-время в секундах. */
fun Long.formatLocalizedDateSeconds(): String = Date(this * 1_000L).formatBySkeleton("yMMdd")

/** Дата в формате текущей локали («12.10.2026»). Вход — Unix-время в миллисекундах. */
fun Long.formatLocalizedDateMillis(): String = Date(this).formatBySkeleton("yMMdd")

private fun Date.formatBySkeleton(skeleton: String): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, skeleton)
    return SimpleDateFormat(pattern, locale).format(this)
}
