package su.afk.yummy.tv.core.model.mutation

/** Durable-очередь мутаций, не доехавших до сервера из-за офлайна. */
interface PendingMutationQueue {

    /**
     * Если [error] — сетевой сбой, ставит [mutation] в очередь, запускает досылку и возвращает
     * true: вызывающая сторона не откатывает оптимистичную запись. Для остальных ошибок ничего не
     * делает и возвращает false.
     */
    suspend fun enqueueOnNetworkFailure(mutation: PendingMutation, error: Throwable): Boolean
}
