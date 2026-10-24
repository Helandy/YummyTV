package su.afk.yummy.tv.feature.details.mobile.relation

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
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationItem
import su.afk.yummy.tv.domain.anime.model.AnimeRelationSubGenre
import su.afk.yummy.tv.feature.details.relation.RelationState
import su.afk.yummy.tv.feature.details.relation.model.RelationType

class RelationMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<RelationState.Event>()

    private val relation = AnimeRelation(
        title = "Kyoto Animation",
        anime = listOf(
            AnimeRelationItem(animeId = 1, title = "Violet Evergarden", poster = null, rating = 8.7, year = 2018),
            AnimeRelationItem(animeId = 2, title = "K-On!", poster = null, rating = 8.1, year = 2009),
        ),
    )

    private fun string(id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

    private fun setScreen(state: RelationState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                RelationMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsRelationTitleAndAnimeCount() {
        setScreen(RelationState.State(relationType = RelationType.STUDIO, isLoading = false, relation = relation))

        composeRule.onNodeWithText("Kyoto Animation").assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.details_related_anime_count, 2)).assertIsDisplayed()
    }

    @Test
    fun animeClickSendsAnimeSelected() {
        setScreen(RelationState.State(relationType = RelationType.STUDIO, isLoading = false, relation = relation))

        composeRule.onNodeWithText("K-On!").performClick()

        assertEquals(listOf<RelationState.Event>(RelationState.Event.AnimeSelected(2)), events)
    }

    @Test
    fun emptyRelationShowsMessage() {
        setScreen(
            RelationState.State(
                relationType = RelationType.GENRE,
                isLoading = false,
                relation = relation.copy(anime = emptyList()),
            ),
        )

        composeRule.onNodeWithText(string(R.string.details_related_empty)).assertIsDisplayed()
    }

    @Test
    fun subGenreClickSendsSubGenreSelected() {
        setScreen(
            RelationState.State(
                relationType = RelationType.GENRE,
                isLoading = false,
                relation = relation.copy(subGenres = listOf(AnimeRelationSubGenre(id = 9, title = "Фэнтези"))),
            ),
        )

        composeRule.onNodeWithText("Фэнтези").performClick()

        assertEquals(listOf<RelationState.Event>(RelationState.Event.SubGenreSelected(9)), events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(
            RelationState.State(
                relationType = RelationType.STUDIO,
                isLoading = false,
                error = UiText.Res(R.string.details_load_error),
            ),
        )

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertEquals(listOf<RelationState.Event>(RelationState.Event.RetrySelected), events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(RelationState.State(relationType = RelationType.STUDIO, isLoading = false, relation = relation))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertEquals(listOf<RelationState.Event>(RelationState.Event.BackSelected), events)
    }
}
