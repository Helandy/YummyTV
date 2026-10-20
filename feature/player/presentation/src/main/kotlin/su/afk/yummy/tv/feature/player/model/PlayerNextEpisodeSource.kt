package su.afk.yummy.tv.feature.player.model

enum class PlayerNextEpisodeSource {
    Controls,
    EndPrompt,

    /** Серия досмотрена, а следующая есть только в другом источнике — показать диалог со списком. */
    EpisodeEnd,

    /** «Смотреть» в диалоге, что в активной озвучке следующей серии нет. */
    UnavailableDialog,
}
