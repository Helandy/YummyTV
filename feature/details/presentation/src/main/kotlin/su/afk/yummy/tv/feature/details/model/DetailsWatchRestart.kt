package su.afk.yummy.tv.feature.details.model

/** Вариант кнопки просмотра, когда доступное уже полностью просмотрено. */
enum class DetailsWatchRestart {
    /** Тайтл завершён — смотреть сначала. */
    Restart,

    /** Онгоинг, новые серии ещё не вышли — смотреть снова. */
    Again,
}
