package su.afk.yummy.tv.core.common.player

/** Итог одного сетевого запроса при получении потока: куда ходили, что ответили и сколько это заняло. */
data class ResolveDetail(
    val host: String,
    /** Код ответа; `null`, если ответа не было вовсе. */
    val httpCode: Int?,
    /** Имя исключения, если запрос оборвался до ответа. */
    val error: String?,
    val elapsedMs: Long,
)
