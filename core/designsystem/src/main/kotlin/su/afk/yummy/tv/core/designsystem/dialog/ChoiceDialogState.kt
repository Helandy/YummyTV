package su.afk.yummy.tv.core.designsystem.dialog

/** Содержимое диалога выбора; вызывающий обновляет его, пока диалог открыт (поиск идёт в фоне). */
sealed interface ChoiceDialogState {
    data object Loading : ChoiceDialogState

    data class Message(val text: String) : ChoiceDialogState

    data class Items(val items: List<ChoiceDialogItem>) : ChoiceDialogState
}
