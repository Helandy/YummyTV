package su.afk.yummy.tv.core.model.mutation

/** Немедленно дослаёт очередь отложенных мутаций на сервер, не дожидаясь WorkManager. */
interface PendingMutationFlusher {

    /** `true` — очередь пуста (всё доставлено или отвергнуто), `false` — осталось то, что нужно повторить. */
    suspend fun flush(): Boolean
}
