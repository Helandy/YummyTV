package su.afk.yummy.tv.domain.account.model

data class AnimeListStats(
    val counts: Map<Int, Int> = emptyMap(),
)
