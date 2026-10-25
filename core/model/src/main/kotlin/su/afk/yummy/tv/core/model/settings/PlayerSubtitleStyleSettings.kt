package su.afk.yummy.tv.core.model.settings

/**
 * Цвета заданы как ARGB-int, а не через `android.graphics.Color`: модуль настроек общий
 * и не должен тянуть Android-типы в модель.
 */
enum class PlayerSubtitleTextColor(val argb: Int) {
    WHITE(0xFFFFFFFF.toInt()),
    YELLOW(0xFFFFEB3B.toInt()),
    CYAN(0xFF80DEEA.toInt()),
    GREEN(0xFF9CCC65.toInt()),
    ORANGE(0xFFFFB74D.toInt()),
    RED(0xFFEF5350.toInt()),
    PINK(0xFFF48FB1.toInt()),
    PURPLE(0xFFCE93D8.toInt()),
    BLUE(0xFF64B5F6.toInt()),
}

enum class PlayerSubtitleBackground(val argb: Int) {
    NONE(0x00000000),
    TRANSLUCENT(0x99000000.toInt()),
    SOLID(0xFF000000.toInt()),
}

/** Оформление субтитров: применяется глобально ко всем источникам, реально сабы отдаёт Alloha. */
data class PlayerSubtitleStyleSettings(
    /** Размер субтитров в процентах от дефолтного размера Media3, 50..200. */
    val textSize: Int = 100,
    val textColor: PlayerSubtitleTextColor = PlayerSubtitleTextColor.WHITE,
    val background: PlayerSubtitleBackground = PlayerSubtitleBackground.TRANSLUCENT,
    /** Отступ субтитров от низа кадра в процентах от его высоты, 0..20. */
    val offset: Int = 6,
    val edge: PlayerSubtitleEdge = PlayerSubtitleEdge.OUTLINE,
)
