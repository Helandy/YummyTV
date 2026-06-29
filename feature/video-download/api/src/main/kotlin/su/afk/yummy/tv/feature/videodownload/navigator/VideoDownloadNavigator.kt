package su.afk.yummy.tv.feature.videodownload.navigator

import androidx.navigation3.runtime.NavKey
import su.afk.yummy.tv.feature.videodownload.IVideoDownloadNavigator

class VideoDownloadNavigator : IVideoDownloadNavigator {
    override fun getVideoDownloadDest(): NavKey = VideoDownloadDestination
}
