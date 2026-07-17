package su.afk.yummy.tv.core.designsystem.presenter.theme

import androidx.compose.ui.graphics.Color

/**
 * Семантические accent-цвета, которых нет в Material [androidx.compose.material3.ColorScheme].
 * Приложение тёмное (пять палитр); значения подобраны под тёмный фон и общие для всех тем.
 * Единый источник, чтобы не дублировать хардкод-цвета по фичам (бейджи оценок, лайки/дизлайки).
 */
object YummySemanticColors {
    /** Оценка 8–10 из 10. */
    val ScoreHigh = Color(0xFF43A866)

    /** Оценка 5–7 из 10. */
    val ScoreMid = Color(0xFFD4A72C)

    /** Контент (текст/иконка) поверх цветного бейджа оценки. */
    val OnScoreBadge = Color.White

    /** Лайк / положительная реакция. */
    val Like = Color(0xFF69D38B)

    /** Дизлайк / отрицательная реакция. */
    val Dislike = Color(0xFFFF6B6B)
}
