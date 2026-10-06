package su.afk.yummy.tv.feature.details.similar

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.analytics.api.AnalyticsTracker
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.anime.AnimeRecommendation
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationReaction
import su.afk.yummy.tv.core.model.anime.AnimeRecommendationVote
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.preferences.settings.YaniAccountSettingsStore
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeRecommendationsUseCase
import su.afk.yummy.tv.domain.anime.usecase.SetAnimeRecommendationIgnoredUseCase
import su.afk.yummy.tv.domain.anime.usecase.VoteAnimeRecommendationUseCase
import su.afk.yummy.tv.feature.details.DetailsAnalytics
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.details.model.SimilarUiState
import su.afk.yummy.tv.feature.details.similar.SimilarState.Effect
import su.afk.yummy.tv.feature.details.similar.SimilarState.Event

class SimilarViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val repository: AnimeRepository = mockk()
    private val settingsStore: YaniAccountSettingsStore = mockk()
    private val stringProvider: StringProvider = mockk()
    private val tracker: AnalyticsTracker = mockk(relaxed = true)
    private val userId = MutableStateFlow(USER_ID)
    private val hiddenIds = MutableStateFlow<Set<Int>>(emptySet())
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { settingsStore.yaniUserId } returns userId
        every { settingsStore.hiddenRecommendationIds } returns hiddenIds
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        coEvery { repository.getAnimeRecommendations(ANIME_ID, false) } returns listOf(recommendation(1), recommendation(2))
        coEvery { repository.getAnimeRecommendations(ANIME_ID, true) } returns listOf(recommendation(3))
    }

    private fun createViewModel() = SimilarViewModel(
        animeId = ANIME_ID,
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getAnimeRecommendations = GetAnimeRecommendationsUseCase(repository),
        setAnimeRecommendationIgnored = SetAnimeRecommendationIgnoredUseCase(repository),
        voteAnimeRecommendation = VoteAnimeRecommendationUseCase(repository),
        settingsStore = settingsStore,
        stringProvider = stringProvider,
        analytics = DetailsAnalytics(tracker),
    )

    private fun recommendation(id: Int, vote: AnimeRecommendationVote = AnimeRecommendationVote.NONE) =
        AnimeRecommendation(animeId = id, title = "t$id", poster = null, rating = null, type = null, year = null, likes = 1, vote = vote)

    private fun SimilarState.State.items() = (similarState as SimilarUiState.Content).items

    @Test
    fun `loads the recommendations`() {
        val state = createViewModel().currentState

        assertEquals(listOf(1, 2), state.items().map { it.animeId })
        assertFalse(state.fromAi)
    }

    @Test
    fun `duplicate titles are collapsed`() {
        coEvery { repository.getAnimeRecommendations(ANIME_ID, false) } returns
            listOf(recommendation(1), recommendation(1), recommendation(2))

        assertEquals(listOf(1, 2), createViewModel().currentState.items().map { it.animeId })
    }

    @Test
    fun `empty answer is an empty state`() {
        coEvery { repository.getAnimeRecommendations(ANIME_ID, false) } returns emptyList()

        assertEquals(SimilarUiState.Empty, createViewModel().currentState.similarState)
    }

    @Test
    fun `failed load is an error state and retry recovers`() {
        coEvery { repository.getAnimeRecommendations(ANIME_ID, false) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals(SimilarUiState.Error("boom"), vm.currentState.similarState)
        coEvery { repository.getAnimeRecommendations(ANIME_ID, false) } returns listOf(recommendation(1))

        vm.setEvent(Event.RetrySelected)

        assertEquals(listOf(1), vm.currentState.items().map { it.animeId })
    }

    @Test
    fun `switching the source loads the ai recommendations`() {
        val vm = createViewModel()

        vm.setEvent(Event.SourceToggled)

        assertTrue(vm.currentState.fromAi)
        assertEquals(listOf(3), vm.currentState.items().map { it.animeId })

        vm.setEvent(Event.SourceSelected(true))
        coVerify(exactly = 1) { repository.getAnimeRecommendations(ANIME_ID, true) }
    }

    @Test
    fun `hidden flag follows the store`() {
        hiddenIds.value = setOf(ANIME_ID)

        assertTrue(createViewModel().currentState.isRecommendationIgnored)
    }

    @Test
    fun `hiding is sent to the server and keeps the new flag`() {
        coEvery { repository.setAnimeRecommendationIgnored(ANIME_ID, true) } returns true
        val vm = createViewModel()

        vm.setEvent(Event.RecommendationVisibilityToggled)

        assertTrue(vm.currentState.isRecommendationIgnored)
        assertFalse(vm.currentState.isRecommendationMutationPending)
    }

    @Test
    fun `rejected hiding restores the flag and shows a toast`() = runTest {
        coEvery { repository.setAnimeRecommendationIgnored(any(), any()) } returns false
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RecommendationVisibilityToggled)

        assertFalse(vm.currentState.isRecommendationIgnored)
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `guest cannot hide or vote`() = runTest {
        userId.value = 0
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.RecommendationVisibilityToggled)
        vm.setEvent(Event.VoteSelected(1, AnimeRecommendationVote.LIKE))

        assertEquals(2, effects.size)
        coVerify(exactly = 0) { repository.setAnimeRecommendationIgnored(any(), any()) }
        coVerify(exactly = 0) { repository.voteAnimeRecommendation(any(), any(), any()) }
    }

    @Test
    fun `vote is replaced by the server reaction`() {
        coEvery { repository.voteAnimeRecommendation(ANIME_ID, 1, AnimeRecommendationVote.LIKE) } returns
            AnimeRecommendationReaction(likes = 10, dislikes = 0, vote = AnimeRecommendationVote.LIKE)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(1, AnimeRecommendationVote.LIKE))

        val item = vm.currentState.items().first { it.animeId == 1 }
        assertEquals(10, item.likes)
        assertEquals(AnimeRecommendationVote.LIKE, item.vote)
        assertTrue(vm.currentState.pendingVoteAnimeIds.isEmpty())
    }

    @Test
    fun `failed vote restores the item and shows a toast`() = runTest {
        coEvery { repository.voteAnimeRecommendation(any(), any(), any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.VoteSelected(1, AnimeRecommendationVote.LIKE))

        assertEquals(recommendation(1), vm.currentState.items().first { it.animeId == 1 })
        assertTrue(vm.currentState.pendingVoteAnimeIds.isEmpty())
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `same vote is sent as none`() {
        coEvery { repository.getAnimeRecommendations(ANIME_ID, false) } returns
            listOf(recommendation(1, AnimeRecommendationVote.LIKE))
        coEvery { repository.voteAnimeRecommendation(ANIME_ID, 1, AnimeRecommendationVote.NONE) } returns
            AnimeRecommendationReaction(0, 0, AnimeRecommendationVote.NONE)
        val vm = createViewModel()

        vm.setEvent(Event.VoteSelected(1, AnimeRecommendationVote.LIKE))

        coVerify(exactly = 1) { repository.voteAnimeRecommendation(ANIME_ID, 1, AnimeRecommendationVote.NONE) }
    }

    @Test
    fun `anime selection and back navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(7))
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { detailsNavigator.getDetailsDest(7) }
        verify(exactly = 1) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val ANIME_ID = 4
        const val USER_ID = 5
    }
}
