package su.afk.yummy.tv.domain.player.repository

import su.afk.yummy.tv.domain.player.model.PlayerStreamResolveResult

/**
 * Точка расширения: модуль-источник превращает свою ссылку `source://…` в прямой поток.
 * Здесь же допустимо показать рекламу или запросить доступ: разрешение потока проходят все входы в плеер.
 */
interface ExtraStreamRepository {
    fun supports(url: String): Boolean

    suspend fun resolve(url: String): PlayerStreamResolveResult

    /**
     * То же, но с [previousUrl] — ссылкой видео, игравшего в плеере перед этим. Источник отличает переход на соседнюю
     * серию своего же источника от входа в него из другой озвучки.
     */
    suspend fun resolve(url: String, previousUrl: String?): PlayerStreamResolveResult = resolve(url)

    /**
     * Забывает сохранённый выбор пользователя для [url], чтобы следующий [resolve] спросил его заново. `true` —
     * у источника есть что спрашивать (плеер перезагрузит поток); `false` — источник выбора не хранит.
     */
    fun forgetSelection(url: String): Boolean = false
}
