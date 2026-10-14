package su.afk.yummy.tv.feature.details.mobile.collections

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.domain.account.model.AnimeCollectionSummary
import su.afk.yummy.tv.feature.details.collections.CollectionsState

class CollectionsMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<CollectionsState.Event>()

    private val collection = AnimeCollectionSummary(
        id = 5,
        title = "Тестовая коллекция",
        description = "",
        posterUrl = null,
        poster = null,
        views = null,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: CollectionsState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                CollectionsMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitleAndEmptyMessage() {
        setScreen(CollectionsState.State(isLoading = false))

        composeRule.onNodeWithText(string(R.string.details_collections_button)).assertIsDisplayed()
        composeRule.onNodeWithText(string(DesignSystemR.string.empty_screen)).assertIsDisplayed()
    }

    @Test
    fun collectionClickSendsCollectionSelected() {
        setScreen(CollectionsState.State(isLoading = false, collections = persistentListOf(collection)))

        composeRule.onNodeWithText("Тестовая коллекция").performClick()

        assertEquals(listOf<CollectionsState.Event>(CollectionsState.Event.CollectionSelected(5)), events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(CollectionsState.State(isLoading = false, error = UiText.Res(R.string.details_load_error)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<CollectionsState.Event>(CollectionsState.Event.RetrySelected), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(CollectionsState.State(isLoading = false))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<CollectionsState.Event>(CollectionsState.Event.BackSelected), events)
    }
}
