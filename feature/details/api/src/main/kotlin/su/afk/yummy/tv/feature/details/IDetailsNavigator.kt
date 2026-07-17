package su.afk.yummy.tv.feature.details

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.details.navigator.DetailsRelationKind

interface IDetailsNavigator {
    fun getDetailsDest(animeId: Int): NavKey
    fun getFullDetailsDest(animeId: Int): NavKey
    fun getEpisodesDest(animeId: Int): NavKey
    fun getEpisodeDubbingsDest(animeId: Int, episode: String): NavKey
    fun getSubscriptionsDest(animeId: Int): NavKey
    fun getTrailersDest(animeId: Int): NavKey
    fun getSimilarDest(animeId: Int): NavKey
    fun getViewingOrderDest(animeId: Int): NavKey
    fun getScreenshotsDest(animeId: Int): NavKey
    fun getRatingDest(animeId: Int): NavKey
    fun getCollectionsDest(animeId: Int): NavKey
    fun getRelationDest(kind: DetailsRelationKind, id: Int, url: String? = null): NavKey
}
