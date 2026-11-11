package su.afk.yummy.tv.domain.player.usecase

import su.afk.yummy.tv.domain.player.repository.ExtraStreamRepository
import javax.inject.Inject

/**
 * Просит источник модуля (`source://…`) забыть сохранённый выбор пользователя для видео, чтобы следующий запуск
 * потока показал выбор заново, например при повторном выборе той же озвучки «Поиск торрента» в плеере.
 * Возвращает `true`, если источник что-то забыл и поток нужно перезагрузить.
 */
class ForgetExtraSourceSelectionUseCase @Inject constructor(
    private val repositories: Set<@JvmSuppressWildcards ExtraStreamRepository>,
) {
    operator fun invoke(iframeUrl: String): Boolean =
        repositories.firstOrNull { it.supports(iframeUrl) }?.forgetSelection(iframeUrl) == true
}
