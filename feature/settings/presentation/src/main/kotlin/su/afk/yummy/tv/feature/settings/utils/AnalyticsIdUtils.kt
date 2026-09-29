package su.afk.yummy.tv.feature.settings.utils

/** Разбивает идентификатор на группы по 3 символа, чтобы его было легко переписать с экрана. */
fun String.groupedByThree(): String = chunked(ANALYTICS_ID_GROUP_SIZE).joinToString(" ")

private const val ANALYTICS_ID_GROUP_SIZE = 3
