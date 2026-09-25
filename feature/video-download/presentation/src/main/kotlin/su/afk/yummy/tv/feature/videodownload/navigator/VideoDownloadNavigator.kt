package su.afk.yummy.tv.feature.videodownload.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator
import javax.inject.Inject

class VideoDownloadNavigator @Inject constructor() : IVideoDownloadNavigator {
    override fun getVideoDownloadDest(): NavKey = VideoDownloadDestination
}
