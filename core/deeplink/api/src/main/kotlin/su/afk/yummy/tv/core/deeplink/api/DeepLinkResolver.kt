package su.afk.yummy.tv.core.deeplink.api

import androidx.navigation3.runtime.NavKey

/**
 * Порт разбора внешней ссылки в экран приложения. Каждая фича регистрирует свой резолвер через
 * Hilt multibinding (`@IntoSet`), поэтому `core:deeplink` не знает, какие схемы и хосты
 * вообще существуют.
 *
 * Реализация возвращает null для всего, что её не касается; резолверы не должны пересекаться
 * по обрабатываемым ссылкам — порядок обхода набора не определён.
 */
fun interface DeepLinkResolver {
    fun resolve(link: DeepLinkReference): NavKey?
}
