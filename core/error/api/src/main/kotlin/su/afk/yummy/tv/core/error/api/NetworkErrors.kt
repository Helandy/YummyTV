package su.afk.yummy.tv.core.error.api

import java.io.IOException

/**
 * Сетевые/оффлайн-ошибки (нет DNS, нет соединения, таймаут) — не баги приложения. Все обрывы
 * соединения (UnknownHostException, ConnectException, SocketTimeoutException и т.п.) — наследники
 * [IOException], поэтому единая проверка здесь используется и для решения "не репортить в
 * аналитику" ([ErrorHandler]), и для решения "поставить мутацию в offline-очередь, а не откатывать".
 */
fun Throwable.isNetworkError(): Boolean = this is IOException
