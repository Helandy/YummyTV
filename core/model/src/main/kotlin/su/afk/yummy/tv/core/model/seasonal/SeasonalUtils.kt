package su.afk.yummy.tv.core.model.seasonal

import java.util.Calendar

private const val HALLOWEEN_FROM_DAY = 20
private const val HALLOWEEN_TO_DAY = 2
private const val NEW_YEAR_FROM_DAY = 15
private const val NEW_YEAR_TO_DAY = 10

/** Праздничная тема для момента [nowMillis]: Хэллоуин 20.10–02.11, Новый год 15.12–10.01. */
fun seasonalThemeAt(nowMillis: Long): SeasonalTheme {
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val month = calendar.get(Calendar.MONTH)
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    return when {
        month == Calendar.OCTOBER && day >= HALLOWEEN_FROM_DAY -> SeasonalTheme.HALLOWEEN
        month == Calendar.NOVEMBER && day <= HALLOWEEN_TO_DAY -> SeasonalTheme.HALLOWEEN
        month == Calendar.DECEMBER && day >= NEW_YEAR_FROM_DAY -> SeasonalTheme.NEW_YEAR
        month == Calendar.JANUARY && day <= NEW_YEAR_TO_DAY -> SeasonalTheme.NEW_YEAR
        else -> SeasonalTheme.NONE
    }
}

/**
 * Ключ праздничного периода для момента [nowMillis]: тема и год начала периода (январь относится
 * к декабрьскому периоду прошлого года). Пустая строка вне праздников. По нему приложение помнит,
 * о каком оформлении пользователю уже сообщили.
 */
fun seasonalAnnouncementKey(nowMillis: Long): String {
    val theme = seasonalThemeAt(nowMillis)
    if (theme == SeasonalTheme.NONE) return ""
    val calendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val year = calendar.get(Calendar.YEAR)
    val startYear = if (calendar.get(Calendar.MONTH) == Calendar.JANUARY) year - 1 else year
    return "${theme.name}-$startYear"
}
