package su.afk.yummy.tv.core.designsystem.dialog

/**
 * Строка списка в [ChoiceDialogCenter]: [id] возвращается вызывающему. [title] — заголовок, [subtitle] —
 * простая подпись, [markers] — заметные зелёные метки «статуса» (например, «уже на сервере») перед остальными,
 * [warnings] — янтарные предупреждения («не заиграет на этом устройстве»), [badges] — чипы-метки (качество, источник), [stats] — строка сведений с цветом по [ChoiceDialogTone].
 */
data class ChoiceDialogItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val badges: List<String> = emptyList(),
    val markers: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val stats: List<ChoiceDialogStat> = emptyList(),
)
