package su.afk.yummy.tv.core.designsystem.dialog

/** Короткое значение в строке сведений карточки (размер, число раздающих…); [tone] красит его по важности. */
data class ChoiceDialogStat(
    val text: String,
    val tone: ChoiceDialogTone = ChoiceDialogTone.Neutral,
)
