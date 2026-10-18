package su.afk.yummy.tv.core.designsystem.components

import su.afk.yummy.tv.core.designsystem.model.TitleListCardUi

/** Год выхода для карточки; `null`, если [showYear] выключен или год неизвестен. */
fun TitleListCardUi.yearText(showYear: Boolean): String? =
    year?.takeIf { showYear && it > 0 }?.toString()
