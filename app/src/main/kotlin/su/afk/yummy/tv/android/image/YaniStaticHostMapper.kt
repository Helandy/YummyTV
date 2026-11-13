package su.afk.yummy.tv.android.image

import coil3.map.Mapper
import coil3.request.Options
import su.afk.yummy.tv.core.model.anime.utils.normalizeYaniStaticUrl

/**
 * Исправляет ссылки на статику yani с хостом (`static-ru.yani.tv`) для любых картинок: постеров,
 * аватарок, новостей. Такие ссылки могли осесть в локальном кэше; без правки они не резолвятся.
 * Для верных ссылок возвращает `null`, чтобы Coil шёл дальше по обычной цепочке.
 */
internal class YaniStaticHostMapper : Mapper<String, String> {
    override fun map(data: String, options: Options): String? =
        normalizeYaniStaticUrl(data).takeIf { it != data }
}
