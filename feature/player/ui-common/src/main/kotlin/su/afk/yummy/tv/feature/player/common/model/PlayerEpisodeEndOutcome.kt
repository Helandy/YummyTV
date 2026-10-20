package su.afk.yummy.tv.feature.player.common.model

/** Чем закончился конец серии в [su.afk.yummy.tv.feature.player.common.PlayerEndFlowState]. */
enum class PlayerEpisodeEndOutcome {
    /** Ничего не показываем. */
    None,

    /** Показан промпт следующей серии или финального действия. */
    PromptShown,

    /** Следующая серия есть только в другом источнике — нужен диалог со списком, его строит VM. */
    SourcesRequested,
}
