package su.afk.yummy.tv.feature.videodownload.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadCacheKeyScheme
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadItem
import su.afk.yummy.tv.domain.videodownload.model.VideoDownloadStatus
import su.afk.yummy.tv.domain.videodownload.model.VideoExportStatus
import su.afk.yummy.tv.feature.videodownload.VideoDownloadState
import su.afk.yummy.tv.feature.videodownload.presentation.R

class VideoDownloadMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<VideoDownloadState.Event>()

    private fun item(status: VideoDownloadStatus) = VideoDownloadItem(
        id = 8L,
        animeId = 44,
        animeTitle = "Скачанное аниме",
        posterUrl = "",
        episode = "3",
        videoId = 1,
        playerName = "Kodik",
        playerId = 1,
        dubbing = "AniDub",
        iframeUrl = "",
        screenshotUrl = "",
        qualityLabel = "720p",
        streamUrl = "",
        headers = emptyMap(),
        cacheKey = "key",
        cacheKeyScheme = VideoDownloadCacheKeyScheme.Namespaced,
        status = status,
        progress = 0.4f,
        bytesDownloaded = 1_000_000L,
        totalBytes = 5_000_000L,
        errorMessage = null,
        exportStatus = VideoExportStatus.Idle,
        exportProgress = 0f,
        exportDirectoryUri = null,
        exportedFileUri = null,
        exportErrorMessage = null,
        createdAt = 0L,
        updatedAt = 0L,
    )

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setScreen(state: VideoDownloadState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                VideoDownloadMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun emptyListShowsTitleAndMessage() {
        setScreen(VideoDownloadState.State())

        composeRule.onNodeWithText(string(R.string.video_download_mobile_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.video_download_empty)).assertIsDisplayed()
    }

    @Test
    fun itemIsShownAndClickSendsItemSelected() {
        setScreen(VideoDownloadState.State(items = persistentListOf(item(VideoDownloadStatus.Downloaded))))

        composeRule.onNodeWithText("Скачанное аниме").assertIsDisplayed()
        composeRule.onNodeWithText("Скачанное аниме").performClick()

        assertTrue(VideoDownloadState.Event.ItemSelected(8L) in events)
    }

    @Test
    fun downloadingItemShowsPauseAllAndSendsEvent() {
        setScreen(VideoDownloadState.State(items = persistentListOf(item(VideoDownloadStatus.Downloading))))

        composeRule.onNodeWithContentDescription(string(R.string.video_download_pause_all)).performClick()

        assertTrue(VideoDownloadState.Event.PauseAllSelected in events)
    }

    @Test
    fun pausedItemDoesNotOfferPauseAll() {
        setScreen(VideoDownloadState.State(items = persistentListOf(item(VideoDownloadStatus.Paused))))

        composeRule.onNodeWithContentDescription(string(R.string.video_download_pause_all)).assertDoesNotExist()
        composeRule.onNodeWithContentDescription(string(R.string.video_download_resume_all)).assertIsDisplayed()
    }

    @Test
    fun deleteDialogConfirmSendsDeleteConfirmed() {
        val downloaded = item(VideoDownloadStatus.Downloaded)
        setScreen(
            VideoDownloadState.State(
                items = persistentListOf(downloaded),
                pendingDeleteItem = downloaded,
            ),
        )

        composeRule.onNodeWithText(string(R.string.video_download_delete_confirm_title, "3")).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_delete)).performClick()

        assertTrue(VideoDownloadState.Event.DeleteConfirmed in events)
    }

    @Test
    fun deleteDialogCancelSendsDeleteDismissed() {
        val downloaded = item(VideoDownloadStatus.Downloaded)
        setScreen(
            VideoDownloadState.State(
                items = persistentListOf(downloaded),
                pendingDeleteItem = downloaded,
            ),
        )

        composeRule.onNodeWithText(string(DesignSystemR.string.common_cancel)).performClick()

        assertTrue(VideoDownloadState.Event.DeleteDismissed in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(VideoDownloadState.State())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(VideoDownloadState.Event.BackSelected in events)
    }
}
