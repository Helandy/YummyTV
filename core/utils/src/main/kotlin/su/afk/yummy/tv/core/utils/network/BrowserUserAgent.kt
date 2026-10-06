package su.afk.yummy.tv.core.utils.network

import su.afk.yummy.tv.core.model.settings.BrowserUserAgentProfile

/**
 * User-Agent приложения для запросов к балансерам и их CDN (кроме Alloha, у которой свой отпечаток
 * сессии). Источник значения — настройка пользователя; потребители читают [userAgent] в момент
 * запроса, а не кэшируют строку.
 *
 * Выбор ограничен списком проверенных профилей ([BrowserUserAgentProfile]), а не свободным
 * текстом и не случайным пулом: okcdn принимает не любой браузерный UA (macOS-Chrome, Safari,
 * Firefox, `okhttp`, пустой — `400`). Раньше UA выбирался `random()` из пула с macOS-строками, и
 * примерно каждый третий запуск не мог играть CVH до перезапуска. Таблица замеров — в
 * `docs/cvh-player.md`.
 */
interface BrowserUserAgentProvider {
    val userAgent: String
}

/**
 * User-Agent по умолчанию. Только для мест, куда провайдер не дотянуть (подстановка UA в
 * заголовки загрузки, когда экстрактор его не передал); всё остальное берёт
 * [BrowserUserAgentProvider].
 */
val BROWSER_USER_AGENT: String = BrowserUserAgentProfile.DEFAULT.userAgent
