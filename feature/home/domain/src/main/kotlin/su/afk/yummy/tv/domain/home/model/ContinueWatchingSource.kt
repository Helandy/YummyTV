package su.afk.yummy.tv.domain.home.model

/** Откуда взята карточка «Продолжить просмотр»: локальный прогресс или серверная история. */
enum class ContinueWatchingSource {
    LOCAL,
    REMOTE,

    /**
     * Следующая серия после досмотренной на сервере: номер +1, позиция 0. Есть ли такая серия,
     * выясняется только при запуске.
     */
    REMOTE_NEXT_EPISODE,
}
