package su.afk.yummy.tv.core.utils.player

private const val RU_PLAYER_PREFIX = "Плеер "
private const val EN_PLAYER_PREFIX = "Player "

/** Имя балансера без служебного префикса «Плеер »/«Player » (у yani оно приходит как «Плеер Kodik»). */
fun String.withoutPlayerPrefix(): String =
    removePrefix(RU_PLAYER_PREFIX).removePrefix(EN_PLAYER_PREFIX)

/** То же, что [withoutPlayerPrefix], но с обрезкой пробелов по краям. */
fun String.playerDisplayName(): String = trim().withoutPlayerPrefix()
