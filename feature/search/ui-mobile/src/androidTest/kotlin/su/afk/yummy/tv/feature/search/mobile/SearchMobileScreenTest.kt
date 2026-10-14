package su.afk.yummy.tv.feature.search.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.search.model.SearchItem
import su.afk.yummy.tv.feature.search.SearchState
import su.afk.yummy.tv.feature.search.navigator.SearchDestination
import su.afk.yummy.tv.feature.search.presentation.R

class SearchMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<SearchState.Event>()

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private val item = SearchItem(id = 90, title = "Найденное аниме", posterUrl = null, rating = 7.5, year = 2021)

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun stateWith(vararg items: SearchItem, active: Boolean = true) = SearchState.State(
        results = flowOf(PagingData.from(items.toList(), sourceLoadStates = loaded)),
        isSearchActive = active,
    )

    private fun setScreen(
        state: SearchState.State,
        destination: SearchDestination = SearchDestination(),
    ) {
        composeRule.setContent {
            ScreenPreviewTheme {
                SearchMobileScreen(
                    dest = destination,
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun typingSendsQueryChanged() {
        setScreen(SearchState.State())

        composeRule.onNode(hasSetTextAction()).performTextInput("наруто")

        assertTrue(SearchState.Event.QueryChanged("наруто") in events)
    }

    @Test
    fun submitButtonSendsSearchSubmitted() {
        setScreen(SearchState.State())

        // подпись «Поиск» есть в топ-баре, в поле и на кнопке: кнопка — кликабельная и не поле ввода
        composeRule
            .onNode(hasText(string(R.string.search_mobile_submit)) and hasClickAction() and !hasSetTextAction())
            .performClick()

        assertTrue(SearchState.Event.SearchSubmitted in events)
    }

    @Test
    fun initialQueryIsSubmittedAsExternalSearch() {
        setScreen(SearchState.State(), SearchDestination(initialQuery = "бродяга"))
        composeRule.waitForIdle()

        assertTrue(SearchState.Event.ExternalSearchSubmitted("бродяга") in events)
    }

    @Test
    fun activeSearchWithoutResultsShowsNothingFound() {
        setScreen(stateWith())

        composeRule.onNodeWithText(string(R.string.search_empty)).assertIsDisplayed()
    }

    @Test
    fun resultsAreShownAndClickSendsItemSelected() {
        setScreen(stateWith(item))

        composeRule.onNodeWithText("Найденное аниме").assertIsDisplayed()
        composeRule.onNodeWithText("Найденное аниме").performClick()

        assertTrue(SearchState.Event.ItemSelected(90) in events)
    }

    @Test
    fun randomButtonSendsRandomAnimeSelected() {
        setScreen(SearchState.State())

        composeRule.onNodeWithContentDescription(string(R.string.search_random_anime)).performClick()

        assertTrue(SearchState.Event.RandomAnimeSelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(SearchState.State())

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(SearchState.Event.BackSelected in events)
    }
}
