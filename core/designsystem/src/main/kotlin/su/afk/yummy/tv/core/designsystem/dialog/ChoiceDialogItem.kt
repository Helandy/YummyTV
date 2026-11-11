package su.afk.yummy.tv.core.designsystem.dialog

/** Строка списка в [ChoiceDialogCenter]: [id] возвращается вызывающему, [title]/[subtitle] показываются пользователю. */
data class ChoiceDialogItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
)
