package su.afk.yummy.tv.core.model.settings

/**
 * Стиль анимации перехода между экранами (вход в тайтл и выход из него, любые другие экраны).
 *
 * [SLIDE] — новый экран выезжает сбоку, предыдущий слегка смещается (параллакс); [FADE_SCALE] —
 * проявление с лёгким масштабом; [NONE] — без анимации.
 */
enum class NavTransitionStyle {
    SLIDE,
    FADE_SCALE,
    NONE,
}
