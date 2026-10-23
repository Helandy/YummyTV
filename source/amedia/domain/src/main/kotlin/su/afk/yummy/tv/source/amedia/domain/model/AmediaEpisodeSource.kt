package su.afk.yummy.tv.source.amedia.domain.model

/** Откуда играет серия в Amedia-плеере: собственный плеер `aser.pro` или Rutube-ролик, который сайт вставил вместо него. */
sealed interface AmediaEpisodeSource {
    /** Запись в плеере `aser.pro/vod/{vodId}`. */
    data class Vod(val vodId: Long) : AmediaEpisodeSource

    /** Rutube-embed (`https://rutube.ru/play/embed/{id}`): воспроизводится штатным Rutube-экстрактором приложения. */
    data class Rutube(val embedUrl: String) : AmediaEpisodeSource
}
