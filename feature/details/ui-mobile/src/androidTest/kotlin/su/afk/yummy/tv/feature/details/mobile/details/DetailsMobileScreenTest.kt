package su.afk.yummy.tv.feature.details.mobile.details

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeGenre
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.model.anime.AnimeScreenshot
import su.afk.yummy.tv.core.model.anime.AnimeTrailer
import kotlinx.collections.immutable.persistentListOf
import su.afk.yummy.tv.feature.details.details.model.DetailsListCount
import su.afk.yummy.tv.feature.details.details.model.DetailsRatingOverview
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.details.DetailsState
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR

class DetailsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<DetailsState.Event>()

    private val details = AnimeDetails(
        id = 1,
        animeUrl = "",
        title = "Тестовое аниме",
        description = "Описание тестового аниме",
        poster = null,
        rating = AnimeRating(average = null, counters = null, kinopoisk = null, shikimori = null, myAnimeList = null),
        genres = listOf(AnimeGenre(id = 3, title = "Драма")),
        year = null,
        ageRating = null,
        views = null,
        status = null,
        type = null,
        episodes = null,
        otherTitles = emptyList(),
        creators = emptyList(),
        studios = emptyList(),
        viewingOrder = emptyList(),
        screenshots = emptyList(),
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: DetailsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                DetailsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun scrollToText(text: String) {
        composeRule.onNode(hasTestTag("details_root") and hasScrollAction())
            .performScrollToNode(hasText(text))
    }

    @Test
    fun missingDetailsShowNotFoundMessage() {
        setScreen(DetailsState.State(isLoading = false, details = null))

        composeRule.onNodeWithText(string(R.string.details_mobile_empty)).assertIsDisplayed()
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(DetailsState.State(isLoading = false, error = UiText.Res(R.string.details_load_error)))

        composeRule.onNodeWithText(string(R.string.details_load_error)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(DetailsState.Event.RetrySelected in events)
    }

    @Test
    fun showsTitleAndGenres() {
        setScreen(DetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithText("Тестовое аниме").assertIsDisplayed()
        composeRule.onNodeWithText("Драма", substring = true).assertIsDisplayed()
    }

    @Test
    fun posterClickSendsPosterClicked() {
        setScreen(DetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithContentDescription("Тестовое аниме").performClick()

        assertTrue(DetailsState.Event.PosterClicked in events)
    }

    @Test
    fun watchButtonSendsWatchSelected() {
        setScreen(DetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithText(string(R.string.details_watch)).performClick()

        assertTrue(DetailsState.Event.WatchSelected in events)
    }

    @Test
    fun descriptionIsShown() {
        setScreen(DetailsState.State(isLoading = false, details = details))

        scrollToText("Описание тестового аниме")
        composeRule.onNodeWithText("Описание тестового аниме").assertIsDisplayed()
    }

    @Test
    fun screenshotInRowSendsScreenshotSelected() {
        val screenshots = listOf(AnimeScreenshot(id = 1, episode = null, small = null, full = null))
        setScreen(DetailsState.State(isLoading = false, details = details.copy(screenshots = screenshots)))

        composeRule.onNode(hasTestTag("details_root") and hasScrollAction())
            .performScrollToNode(hasTestTag("details_screenshots_row"))
        composeRule.onNodeWithContentDescription(string(R.string.details_mobile_screenshot)).performClick()

        assertTrue(DetailsState.Event.ScreenshotSelected(0) in events)
    }

    @Test
    fun videoCardsOpenTrailersAndBloggerVideos() {
        setScreen(
            DetailsState.State(
                isLoading = false,
                details = details,
                trailers = persistentListOf(AnimeTrailer(iframeUrl = "https://www.youtube.com/embed/abc")),
                isExtrasLoading = false,
            ),
        )

        composeRule.onNode(hasTestTag("details_root") and hasScrollAction())
            .performScrollToNode(hasTestTag("details_trailers_card"))
        composeRule.onNode(hasTestTag("details_trailers_card")).performClick()
        composeRule.onNode(hasTestTag("details_blogger_videos_card")).performClick()

        assertTrue(DetailsState.Event.TrailersSelected in events)
        assertTrue(DetailsState.Event.BloggerVideosSelected in events)
    }

    @Test
    fun ratingCardShowsListsAndOpensRatingScreen() {
        setScreen(
            DetailsState.State(
                isLoading = false,
                details = details,
                ratingOverview = DetailsRatingOverview(
                    average = 8.0,
                    votes = 2,
                    distribution = persistentListOf(0, 0, 0, 0, 0, 0, 0, 2, 0, 0),
                    listCounts = persistentListOf(DetailsListCount(UserAnimeList.WATCHING, 7)),
                ),
            ),
        )

        composeRule.onNode(hasTestTag("details_root") and hasScrollAction())
            .performScrollToNode(hasTestTag("details_rating_card"))
        composeRule.onNodeWithText(string(R.string.details_library_list_watching), substring = true).assertIsDisplayed()
        composeRule.onNode(hasTestTag("details_rating_card")).performClick()

        assertTrue(DetailsState.Event.RatingScreenSelected in events)
    }

    @Test
    fun loadingExtrasShowSkeletons() {
        setScreen(DetailsState.State(isLoading = false, details = details, isExtrasLoading = true))

        composeRule.onNode(hasTestTag("details_root") and hasScrollAction())
            .performScrollToNode(hasTestTag("details_rating_skeleton"))
        composeRule.onNode(hasTestTag("details_rating_skeleton")).assertIsDisplayed()
        composeRule.onNode(hasTestTag("details_trailers_skeleton")).assertExists()
    }

    @Test
    fun commentsButtonSendsCommentsSelected() {
        setScreen(DetailsState.State(isLoading = false, details = details))

        scrollToText(string(R.string.details_mobile_show_comments))
        composeRule.onNodeWithText(string(R.string.details_mobile_show_comments)).performClick()

        assertTrue(DetailsState.Event.CommentsSelected in events)
    }

    @Test
    fun libraryPickerSendsLibraryListSelected() {
        setScreen(DetailsState.State(isLoading = false, details = details, showLibraryListPicker = true))

        composeRule.onNodeWithText(string(R.string.details_library_list_watching)).performClick()

        assertTrue(DetailsState.Event.LibraryListSelected(UserAnimeList.WATCHING) in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(DetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(DetailsState.Event.BackSelected in events)
    }
}
