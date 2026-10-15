package su.afk.yummy.tv.domain.anime.repository

import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeVideo

/**
 * Точка расширения: отдельные модули-источники добавляют свои видео к выдаче `/videos`.
 * Без подключённых модулей набор реализаций пуст и поведение не меняется.
 */
interface ExtraVideoRepository {
    /**
     * Видео этого источника для тайтла. Пустой список = источника нет, в UI он не показывается.
     * `iframeUrl` должен иметь схему `source://`: прямая ссылка на файл появляется только при разрешении потока в плеере.
     */
    suspend fun videos(details: AnimeDetails): List<AnimeVideo>

    /**
     * Подтверждение перед открытием плеера (например, согласие на рекламу). Вызывается для любого видео:
     * чужие источники возвращают `true`. `false` отменяет переход, пользователь остаётся на деталке.
     */
    suspend fun confirmOpen(video: AnimeVideo): Boolean = true
}
