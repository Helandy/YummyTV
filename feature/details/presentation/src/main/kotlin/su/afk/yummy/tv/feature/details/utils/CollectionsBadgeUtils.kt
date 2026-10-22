package su.afk.yummy.tv.feature.details.utils

/**
 * Текст бэйджа числа коллекций. API отдаёт страницу без общего числа, поэтому полная страница
 * ([pageLimit] штук) показывается как «N+»; пустой список — без бэйджа.
 */
internal fun collectionsBadge(count: Int, pageLimit: Int): String? = when {
    count <= 0 -> null
    count >= pageLimit -> "$pageLimit+"
    else -> count.toString()
}
