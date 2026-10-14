package su.afk.yummy.tv.feature.details.mobile.full

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.core.model.anime.AnimeDetails
import su.afk.yummy.tv.core.model.anime.AnimeGenre
import su.afk.yummy.tv.core.model.anime.AnimePerson
import su.afk.yummy.tv.core.model.anime.AnimeRating
import su.afk.yummy.tv.core.model.anime.AnimeStudio
import su.afk.yummy.tv.feature.details.full.FullDetailsState

class FullDetailsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<FullDetailsState.Event>()

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
        otherTitles = listOf("Test Anime"),
        creators = listOf(AnimePerson(id = 5, title = "Режиссёр Тест")),
        studios = listOf(AnimeStudio(id = 4, title = "Студия Тест", url = "studio-url")),
        viewingOrder = emptyList(),
        screenshots = emptyList(),
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: FullDetailsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                FullDetailsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndDescription() {
        setScreen(FullDetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithText(string(R.string.details_full_description)).assertIsDisplayed()
        composeRule.onNodeWithText("Тестовое аниме").assertIsDisplayed()
        composeRule.onNodeWithText("Описание тестового аниме").assertIsDisplayed()
    }

    @Test
    fun missingDetailsShowEmptyMessage() {
        setScreen(FullDetailsState.State(isLoading = false, details = null))

        composeRule.onNodeWithText(string(DesignSystemR.string.empty_screen)).assertIsDisplayed()
    }

    @Test
    fun genreClickSendsGenreSelected() {
        setScreen(FullDetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithText("Драма").performClick()

        assertEquals(listOf<FullDetailsState.Event>(FullDetailsState.Event.GenreSelected(3)), events)
    }

    @Test
    fun studioClickSendsStudioSelected() {
        setScreen(FullDetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithText("Студия Тест").performClick()

        assertEquals(listOf<FullDetailsState.Event>(FullDetailsState.Event.StudioSelected(4, "studio-url")), events)
    }

    @Test
    fun creatorClickSendsDirectorSelected() {
        setScreen(FullDetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithText("Режиссёр Тест").performClick()

        assertEquals(listOf<FullDetailsState.Event>(FullDetailsState.Event.DirectorSelected(5)), events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(FullDetailsState.State(isLoading = false, error = UiText.Res(R.string.details_load_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<FullDetailsState.Event>(FullDetailsState.Event.RetrySelected), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(FullDetailsState.State(isLoading = false, details = details))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<FullDetailsState.Event>(FullDetailsState.Event.BackSelected), events)
    }
}
