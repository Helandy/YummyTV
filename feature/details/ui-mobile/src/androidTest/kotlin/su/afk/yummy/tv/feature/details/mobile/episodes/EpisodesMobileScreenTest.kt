package su.afk.yummy.tv.feature.details.mobile.episodes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.anime.AnimeVideo
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.details.model.VideosUiState
import su.afk.yummy.tv.feature.details.episodes.EpisodesState
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class EpisodesMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<EpisodesState.Event>()

    private val video = AnimeVideo(
        id = 501,
        episode = "3",
        dubbing = "AniDub",
        player = "Kodik",
        playerId = 1,
        iframeUrl = "https://kodik.example/seria/501",
        durationSeconds = 1400,
    )

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun withEpisode() = EpisodesState.State(
        videosState = VideosUiState.Content(persistentListOf(video)),
        episodeGroups = persistentListOf(
            EpisodesState.EpisodeGroup(episode = "3", videos = persistentListOf(video)),
        ),
    )

    private fun setScreen(state: EpisodesState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                EpisodesMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun emptyVideosShowMessage() {
        setScreen(EpisodesState.State(videosState = VideosUiState.Empty))

        composeRule.onNodeWithText(string(R.string.details_episodes)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_episodes_empty)).assertIsDisplayed()
    }

    @Test
    fun loadingHidesEmptyMessage() {
        setScreen(EpisodesState.State(videosState = VideosUiState.Loading))

        composeRule.onNodeWithText(string(R.string.details_episodes_empty)).assertDoesNotExist()
    }

    @Test
    fun errorRetrySendsRetryVideosSelected() {
        setScreen(
            EpisodesState.State(videosState = VideosUiState.Error(UiText.Res(R.string.details_load_error))),
        )

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(EpisodesState.Event.RetryVideosSelected in events)
    }

    @Test
    fun episodeCardIsShown() {
        setScreen(withEpisode())

        composeRule.onNodeWithText(string(R.string.details_episode_number, "3")).assertIsDisplayed()
    }

    @Test
    fun episodeClickSendsEpisodeSelected() {
        setScreen(withEpisode())

        composeRule.onNodeWithText(string(R.string.details_episode_number, "3")).performClick()

        assertTrue(EpisodesState.Event.EpisodeSelected(video) in events)
    }

    @Test
    fun episodeLongClickSendsEpisodeActionsRequested() {
        setScreen(withEpisode())

        composeRule.onNodeWithText(string(R.string.details_episode_number, "3"))
            .performTouchInput { longClick() }

        assertTrue(events.any { it is EpisodesState.Event.EpisodeActionsRequested })
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(EpisodesState.State(videosState = VideosUiState.Empty))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(EpisodesState.Event.BackSelected in events)
    }
}
