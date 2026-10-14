package su.afk.yummy.tv.feature.details.details.model

import su.afk.yummy.tv.core.model.anime.AnimeVideo

/** UI action produced by details/episodes player source selection. */
internal sealed interface DetailsPlayerSelection {
    data class Navigate(val video: AnimeVideo) : DetailsPlayerSelection
    data class ShowPicker(val picker: BalancerPickerState) : DetailsPlayerSelection
}
