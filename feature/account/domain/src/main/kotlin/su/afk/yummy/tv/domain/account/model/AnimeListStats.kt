package su.afk.yummy.tv.domain.account

data class AnimeListStats(
    val counts: Map<Int, Int> = emptyMap(),
)
