package su.afk.yummy.tv.feature.library.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.feature.library.LibraryState
import su.afk.yummy.tv.feature.library.model.LibraryTab
import su.afk.yummy.tv.feature.library.presentation.R

class LibraryMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<LibraryState.Event>()

    private val item = LibraryItem(animeId = 33, title = "Любимое аниме", isFavorite = true)

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun favoritesState(vararg items: LibraryItem) = LibraryState.State(
        selectedTab = LibraryTab.FAVORITES,
        tabItems = persistentMapOf(LibraryTab.FAVORITES to persistentListOf(*items)),
    )

    private fun setScreen(state: LibraryState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                LibraryMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun screenResumedIsSentOnOpen() {
        setScreen(LibraryState.State())
        composeRule.waitForIdle()

        assertTrue(LibraryState.Event.ScreenResumed in events)
    }

    @Test
    fun emptyFavoritesShowsMessage() {
        setScreen(favoritesState())

        composeRule.onNodeWithText(string(R.string.library_empty_list)).assertIsDisplayed()
    }

    @Test
    fun itemIsShownAndClickSendsAnimeSelected() {
        setScreen(favoritesState(item))

        composeRule.onNodeWithText("Любимое аниме").assertIsDisplayed()
        composeRule.onNodeWithText("Любимое аниме").performClick()

        assertTrue(LibraryState.Event.AnimeSelected(33) in events)
    }

    @Test
    fun deleteAsksForConfirmation() {
        setScreen(favoritesState(item))

        composeRule.onNodeWithContentDescription(string(R.string.library_mobile_remove_content_description))
            .performClick()

        composeRule.onNodeWithText(string(R.string.library_mobile_remove_title)).assertIsDisplayed()
        assertTrue(events.none { it is LibraryState.Event.RemoveEntry })
    }

    @Test
    fun confirmedDeleteSendsRemoveEntry() {
        setScreen(favoritesState(item))

        composeRule.onNodeWithContentDescription(string(R.string.library_mobile_remove_content_description))
            .performClick()
        // подпись кнопки подтверждения совпадает с описанием кнопки удаления в списке
        composeRule.onAllNodesWithText(string(R.string.library_mobile_remove_content_description)).onFirst()
            .performClick()

        assertTrue(events.any { it is LibraryState.Event.RemoveEntry })
    }

    @Test
    fun tabClickSendsTabSelected() {
        setScreen(LibraryState.State())

        composeRule.onNodeWithText(string(R.string.library_tab_watching), substring = true).performClick()
        composeRule.waitForIdle()

        assertTrue(LibraryState.Event.TabSelected(LibraryTab.WATCHING) in events)
    }
}
