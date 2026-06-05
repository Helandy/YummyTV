package su.afk.yummy.tv.feature.details.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.details.IDetailsNavigator

class DetailsNavigator : IDetailsNavigator {
    override fun getDetailsDest(animeId: Int): NavKey = DetailsDestination(animeId)
    override fun getFullDetailsDest(animeId: Int): NavKey = DetailsFullDestination(animeId)
    override fun getEpisodesDest(animeId: Int): NavKey = DetailsEpisodesDestination(animeId)
    override fun getEpisodeDubbingsDest(animeId: Int, episode: String): NavKey =
        DetailsEpisodeDubbingsDestination(animeId = animeId, episode = episode)
    override fun getTrailersDest(animeId: Int): NavKey = DetailsTrailersDestination(animeId)
    override fun getSimilarDest(animeId: Int): NavKey = DetailsSimilarDestination(animeId)
    override fun getViewingOrderDest(animeId: Int): NavKey = DetailsViewingOrderDestination(animeId)
    override fun getScreenshotsDest(animeId: Int): NavKey = DetailsScreenshotsDestination(animeId)
    override fun getRatingDest(animeId: Int): NavKey = DetailsRatingDestination(animeId)
    override fun getCollectionsDest(animeId: Int): NavKey = DetailsCollectionsDestination(animeId)
}
