package su.afk.yummy.tv.core.utils.network

/**
 * `Accept-Language` для запросов к балансерам, которые отдают страницу плеера только «русскому»
 * браузеру. Без него и без `Accept-Encoding` Zedfilm отвечает 404 «Видео не найдено» на живую
 * ссылку. Alloha этой константой не пользуется: у её прокси свой заголовок, повторяющий отпечаток
 * Chrome (`AllohaStreamProxy.DEFAULT_ACCEPT_LANGUAGE`).
 */
const val RU_ACCEPT_LANGUAGE: String = "ru-RU,ru;q=0.9,en;q=0.8"
