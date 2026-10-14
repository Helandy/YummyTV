package su.afk.yummy.tv.feature.collection.mobile.catalog

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
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
import su.afk.yummy.tv.domain.collection.model.CollectionSummary
import su.afk.yummy.tv.feature.collection.catalog.CollectionsCatalogState
import su.afk.yummy.tv.feature.collection.presentation.R

class CollectionsCatalogMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<CollectionsCatalogState.Event>()

    private val loaded = LoadStates(
        refresh = LoadState.NotLoading(endOfPaginationReached = false),
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = LoadState.NotLoading(endOfPaginationReached = true),
    )

    private val summary = CollectionSummary(
        id = 5,
        title = "Каталожная коллекция",
        description = "",
        posterUrl = null,
        likesCount = 3,
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: CollectionsCatalogState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                CollectionsCatalogMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    private fun stateWith(vararg items: CollectionSummary) = CollectionsCatalogState.State(
        items = flowOf(PagingData.from(items.toList(), sourceLoadStates = loaded)),
    )

    @Test
    fun showsTitleAndCollection() {
        setScreen(stateWith(summary))

        composeRule.onNodeWithText(string(R.string.collection_catalog_title)).assertIsDisplayed()
        composeRule.onNodeWithText("Каталожная коллекция").assertIsDisplayed()
    }

    @Test
    fun collectionClickSendsCollectionSelected() {
        setScreen(stateWith(summary))

        composeRule.onNodeWithText("Каталожная коллекция").performClick()

        assertTrue(CollectionsCatalogState.Event.CollectionSelected(5) in events)
    }

    @Test
    fun createDialogTypingSendsTitleChanged() {
        setScreen(stateWith().copy(isCreateDialogVisible = true))

        composeRule.onNodeWithText(string(R.string.collection_create_dialog_title)).assertIsDisplayed()
        composeRule.onAllNodes(hasSetTextAction())[0].performTextInput("Новая")

        assertTrue(CollectionsCatalogState.Event.CreateTitleChanged("Новая") in events)
    }

    @Test
    fun createDialogConfirmSendsCreateConfirmed() {
        setScreen(stateWith().copy(isCreateDialogVisible = true, createTitle = "Новая"))

        composeRule.onNodeWithText(string(R.string.collection_create_confirm)).performClick()

        assertTrue(CollectionsCatalogState.Event.CreateConfirmed in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(stateWith(summary))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(CollectionsCatalogState.Event.BackSelected in events)
    }
}
