package su.afk.yummy.tv.core.storage.maintenance

/** Абстракция над плановой очисткой устаревших кэшей — позволяет подменять реализацию в тестах. */
interface StorageCleanup {

    suspend fun purgeStaleCaches(now: Long = System.currentTimeMillis())

    /**
     * Сбрасывает все таблицы-кэши разом, независимо от возраста. Пользовательские данные (библиотека, прогресс
     * просмотра, «продолжить», загрузки, отложенные изменения) не затрагиваются.
     */
    suspend fun purgeAllCaches()
}
