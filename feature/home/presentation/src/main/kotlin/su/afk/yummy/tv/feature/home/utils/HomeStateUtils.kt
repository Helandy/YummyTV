package su.afk.yummy.tv.feature.home.utils

import su.afk.yummy.tv.feature.home.HomeState

/** Лента и «продолжить просмотр» загружены — главную можно показывать целиком. */
fun HomeState.State.hasInitialContent(): Boolean =
    !isLoading && feed != null && isContinueWatchingLoaded

/**
 * Первый экран главной окончательно отрисован: контент или ошибка вместо лоадера.
 * По этому условию экраны вызывают reportFullyDrawn — отсюда TTFD старта.
 */
fun HomeState.State.isFirstScreenSettled(): Boolean = error != null || hasInitialContent()
