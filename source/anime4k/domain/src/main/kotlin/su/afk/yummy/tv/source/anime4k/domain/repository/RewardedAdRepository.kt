package su.afk.yummy.tv.source.anime4k.domain.repository

interface RewardedAdRepository {
    /** Настроена ли реклама (есть рекламный блок). Без неё источник не показывается. */
    val isAvailable: Boolean

    /**
     * Сообщает пользователю, что 4K доступен только после рекламы, и спрашивает согласие.
     * `false`: отказался или показать вопрос не удалось.
     */
    suspend fun askConsent(): Boolean

    /**
     * То же согласие, но заданное до перехода в плеер. Положительный ответ запоминается на короткое время,
     * чтобы следующий [askConsent] (уже в плеере) не спрашивал второй раз.
     */
    suspend fun confirmBeforeOpen(): Boolean

    /** Показывает рекламу с вознаграждением; `true`, только если пользователь получил награду. */
    suspend fun showRewardedAd(): Boolean
}
