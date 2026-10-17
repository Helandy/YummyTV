package su.afk.yummy.tv.core.common.widget

/**
 * Порт перерисовки виджетов рабочего стола из фоновых задач. Реализация живёт в `app`, где лежат
 * сами виджеты, поэтому воркеры из core-модулей обновляют их, не зная о Glance.
 */
interface HomeScreenWidgetRefresher {

    /** Перерисовывает все размещённые виджеты; без виджетов ничего не делает. */
    suspend fun refresh()
}
