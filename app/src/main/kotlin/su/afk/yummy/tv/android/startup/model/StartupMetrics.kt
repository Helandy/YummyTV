package su.afk.yummy.tv.android.startup.model

/**
 * Замер одного холодного старта. Все длительности отсчитываются от старта процесса.
 *
 * [ttfdMs] пустой, если главная так и не отрисовалась до ухода пользователя или таймаута
 * (например, диплинк сразу в детали). [startType]/[startReason] есть только на API 35+.
 */
data class StartupMetrics(
    val ui: StartupUi,
    val entry: StartupEntry,
    val appOnCreateMs: Long,
    val ttidMs: Long,
    val ttfdMs: Long?,
    val ramGb: Int,
    val apiLevel: Int,
    val startType: String?,
    val startReason: String?,
)
