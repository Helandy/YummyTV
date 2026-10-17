package su.afk.yummy.tv.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * Именованные базовые цвета, используемые в [YummySemanticColors]. Один hex-литерал — одно
 * имя, чтобы одинаковые значения переиспользовались вместо повторного хардкода по фичам.
 */
private object YummyPalette {
    val Red = Color(0xFFFF6B6B)
    val Purple = Color(0xFFA678E8)
    val Green = Color(0xFF69D38B)
    val Amber = Color(0xFFFFC857)
    val Gray = Color(0xFF9CA3AF)
    val Magenta = Color(0xFFD86BFF)
    val ForestGreen = Color(0xFF4CAF50)
    val ScoreHighGreen = Color(0xFF43A866)
    val ScoreMidAmber = Color(0xFFD4A72C)
    val RatingRed = Color(0xFFE53935)
    val RatingGreen = Color(0xFF69F0AE)
    val ScrimOverlay = Color(0xF21B1B1F)
    val ScrimPanel = Color(0xE6121214)
}

/**
 * Семантические accent-цвета, которых нет в Material [androidx.compose.material3.ColorScheme].
 * Приложение тёмное (пять палитр); значения подобраны под тёмный фон и общие для всех тем.
 * Единый источник, чтобы не дублировать хардкод-цвета по фичам (бейджи оценок, лайки/дизлайки,
 * статусы списка, оверлеи).
 */
object YummySemanticColors {
    /** Оценка 8–10 из 10. */
    val ScoreHigh = YummyPalette.ScoreHighGreen

    /** Оценка 5–7 из 10. */
    val ScoreMid = YummyPalette.ScoreMidAmber

    /** Контент (текст/иконка) поверх цветного бейджа оценки. */
    val OnScoreBadge = Color.White

    /** Лайк / положительная реакция. */
    val Like = YummyPalette.Green

    /** Дизлайк / отрицательная реакция. */
    val Dislike = YummyPalette.Red

    /**
     * Текст необратимого действия («Удалить») в диалогах: `colorScheme.error` в тёмных темах
     * бледно-розовый и не читается как предупреждение.
     */
    val Destructive = YummyPalette.Red

    /** Низкий рейтинг (бейдж оценки). */
    val RatingBadgeLow = YummyPalette.RatingRed

    /** Высокий рейтинг (бейдж оценки). */
    val RatingBadgeHigh = YummyPalette.RatingGreen

    /** Статус пользовательского списка: смотрю. */
    val StatusWatching = YummyPalette.Red

    /** Статус пользовательского списка: запланировано. */
    val StatusPlanned = YummyPalette.Purple

    /** Статус пользовательского списка: просмотрено. */
    val StatusCompleted = YummyPalette.Green

    /** Статус пользовательского списка: отложено. */
    val StatusPostponed = YummyPalette.Amber

    /** Статус пользовательского списка: брошено. */
    val StatusDropped = YummyPalette.Gray

    /** Статус пользовательского списка: избранное. */
    val StatusFavorite = YummyPalette.Magenta

    /** Контент в процессе (продолжить просмотр, идёт эфир, активная загрузка). */
    val InProgress = YummyPalette.ForestGreen

    /** Затемняющая подложка полноэкранного оверлея (TV picker). */
    val OverlayScrim = YummyPalette.ScrimOverlay

    /** Затемняющая подложка выезжающей панели (TV player controls). */
    val PanelScrim = YummyPalette.ScrimPanel
}
