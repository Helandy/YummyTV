package su.afk.yummy.tv.core.designsystem.dialog

/** Содержимое диалога выбора; вызывающий обновляет его, пока диалог открыт (поиск идёт в фоне). */
sealed interface ChoiceDialogState {
    data object Loading : ChoiceDialogState

    /** Загрузка с подписью текущего шага («Запрос источников…»); вызывающий обновляет [text] по ходу работы. */
    data class Progress(val text: String) : ChoiceDialogState

    /**
     * Выбор сделан без пользователя: диалог закрывается, а [ChoiceDialogCenter.pick] возвращает [id]. Пока это
     * состояние не пришло, диалог показывает загрузку.
     */
    data class AutoSelect(val id: String) : ChoiceDialogState

    /** Сообщение; если задан [retryText], под ним показывается кнопка повтора (ответ [ChoiceDialogCenter.RETRY_ID]). */
    data class Message(val text: String, val retryText: String? = null) : ChoiceDialogState

    data class Items(val items: List<ChoiceDialogItem>) : ChoiceDialogState
}
