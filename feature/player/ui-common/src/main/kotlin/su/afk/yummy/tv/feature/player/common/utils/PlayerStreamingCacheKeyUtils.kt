package su.afk.yummy.tv.feature.player.common.utils

import android.net.Uri

/**
 * Стабильный ключ кэша для подписанной прямой ссылки okcdn.
 *
 * Узел и подпись меняются при каждом перерезолве и при переезде на резервный узел CDN, а файл при
 * этом тот же: подпись okcdn от хоста не зависит. Ключом по умолчанию служит полный URI, поэтому
 * без нормализации уже скачанный буфер после каждой смены ссылки становится недостижим, а кэш
 * забивается копиями одного файла.
 *
 * Идентичность — `id` (видео), `type` (вариант качества) и путь; `expires`, `sig`, `srcIp`, `ms`,
 * `srcAg` и хост исключены. Путь нужен обязательно: `type` совпадает у разных форматов одного
 * видео (у HLS и 480p это `2`, у DASH и 360p — `1`), и различает их только он.
 *
 * Разбор параметров ссылки — в `docs/cvh-player.md`.
 *
 * @return null, если ссылка не похожа на подписанную ссылку okcdn — такую отдаём ключу по умолчанию.
 */
fun okCdnStableCacheKey(uri: Uri): String? {
    val id = uri.getQueryParameter(QUERY_ID)?.takeIf { it.isNotBlank() } ?: return null
    val type = uri.getQueryParameter(QUERY_TYPE)?.takeIf { it.isNotBlank() } ?: return null
    val path = uri.path?.takeIf { it.isNotBlank() && it != "/" }.orEmpty()
    return "$CACHE_KEY_PREFIX|$id|$type|$path"
}

private const val CACHE_KEY_PREFIX = "okcdn"
private const val QUERY_ID = "id"
private const val QUERY_TYPE = "type"
