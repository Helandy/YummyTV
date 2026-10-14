package su.afk.yummy.tv.feature.schedule.mobile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import java.time.LocalDate
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.R as DesignSystemR
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.core.model.text.UiText
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleItem
import su.afk.yummy.tv.feature.schedule.ScheduleState
import su.afk.yummy.tv.feature.schedule.model.ScheduleDayUi
import su.afk.yummy.tv.feature.schedule.model.ScheduleReleaseUi
import su.afk.yummy.tv.feature.schedule.model.ScheduleTimelineUi
import su.afk.yummy.tv.feature.schedule.presentation.R

class ScheduleMobileScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val events = mutableListOf<ScheduleState.Event>()

    private val firstDay = LocalDate.of(2026, 10, 12)

    private fun release(animeId: Int, title: String) = ScheduleReleaseUi(
        item = AnimeScheduleItem(
            animeId = animeId,
            title = title,
            posterUrl = null,
            nextDateEpochSeconds = null,
            airedEpisodes = 3,
            totalEpisodes = 12,
        ),
        epochSeconds = 1_760_000_000L,
        episode = 4,
        aired = false,
    )

    private val timeline = ScheduleTimelineUi(
        dayGroups = listOf(
            ScheduleDayUi(date = firstDay, weekdayLabel = "Пн", items = listOf(release(21, "Первое аниме"))),
            ScheduleDayUi(date = firstDay.plusDays(1), weekdayLabel = "Вт", items = listOf(release(22, "Второе аниме"))),
        ),
        selectedEpochDay = firstDay.toEpochDay(),
    )

    private fun string(id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.getString(id)

    private fun setScreen(state: ScheduleState.State) {
        composeRule.setContent {
            ScreenPreviewTheme {
                ScheduleMobileScreen(
                    state = state,
                    effect = emptyFlow(),
                    onEvent = { events += it },
                )
            }
        }
    }

    @Test
    fun showsTitle() {
        setScreen(ScheduleState.State(isLoading = false, tvSchedule = timeline))

        composeRule.onNodeWithText(string(R.string.schedule_mobile_title)).assertIsDisplayed()
    }

    @Test
    fun selectedDayShowsReleases() {
        setScreen(ScheduleState.State(isLoading = false, tvSchedule = timeline))

        composeRule.onNodeWithText("Первое аниме").assertIsDisplayed()
    }

    @Test
    fun releaseClickSendsAnimeSelected() {
        setScreen(ScheduleState.State(isLoading = false, tvSchedule = timeline))

        composeRule.onNodeWithText("Первое аниме").performClick()

        assertTrue(ScheduleState.Event.AnimeSelected(21) in events)
    }

    @Test
    fun dayChipSendsDateSelected() {
        setScreen(ScheduleState.State(isLoading = false, tvSchedule = timeline))

        composeRule.onNodeWithText("Вт", substring = true).performClick()
        composeRule.waitForIdle()

        assertTrue(ScheduleState.Event.DateSelected(firstDay.plusDays(1).toEpochDay()) in events)
    }

    @Test
    fun errorRetrySendsRetrySelected() {
        setScreen(ScheduleState.State(isLoading = false, error = UiText.Res(R.string.schedule_mobile_title)))

        composeRule.onNodeWithText(string(DesignSystemR.string.common_retry)).performClick()

        assertTrue(ScheduleState.Event.RetrySelected in events)
    }

    @Test
    fun backButtonSendsBackSelected() {
        setScreen(ScheduleState.State(isLoading = false, tvSchedule = timeline))

        composeRule.onNodeWithContentDescription(string(DesignSystemR.string.common_back)).performClick()

        assertTrue(ScheduleState.Event.BackSelected in events)
    }
}
