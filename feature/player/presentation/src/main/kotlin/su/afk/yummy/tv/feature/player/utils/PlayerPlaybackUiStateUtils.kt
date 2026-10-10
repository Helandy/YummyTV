package su.afk.yummy.tv.feature.player.utils

import su.afk.yummy.tv.feature.player.model.PlayerPlaybackUiState

/** Есть куда переходить после конца серии: следующая серия либо та же серия в другой озвучке. */
val PlayerPlaybackUiState.canPlayNext: Boolean
    get() = hasNextEpisode || nextEpisodeDubbing != null
