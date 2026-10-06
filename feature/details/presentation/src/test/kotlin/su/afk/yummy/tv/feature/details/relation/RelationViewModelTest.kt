package su.afk.yummy.tv.feature.details.relation

import androidx.navigation3.runtime.NavKey
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.model.ErrorItem
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.anime.model.AnimeRelation
import su.afk.yummy.tv.domain.anime.model.AnimeRelationKind
import su.afk.yummy.tv.domain.anime.model.AnimeRelationReference
import su.afk.yummy.tv.domain.anime.repository.AnimeRepository
import su.afk.yummy.tv.domain.anime.usecase.GetAnimeRelationUseCase
import su.afk.yummy.tv.feature.details.IDetailsNavigator
import su.afk.yummy.tv.feature.details.navigator.DetailsRelationKind
import su.afk.yummy.tv.feature.details.relation.RelationState.Event
import su.afk.yummy.tv.feature.details.relation.model.RelationType

class RelationViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk()
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val detailsNavigator: IDetailsNavigator = mockk()
    private val repository: AnimeRepository = mockk()
    private val stringProvider: StringProvider = mockk(relaxed = true)
    private val relation: AnimeRelation = mockk()
    private val navKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { errorHandler.parse(any(), any(), any(), any()) } returns ErrorItem(title = "title", message = "parsed message")
        every { detailsNavigator.getDetailsDest(any()) } returns navKey
        every { detailsNavigator.getRelationDest(any(), any(), any()) } returns navKey
        coEvery { repository.getAnimeRelation(any()) } returns relation
    }

    private fun createViewModel(kind: DetailsRelationKind = DetailsRelationKind.STUDIO) = RelationViewModel(
        kind = kind,
        id = RELATION_ID,
        url = "url",
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        detailsNavigator = detailsNavigator,
        getAnimeRelation = GetAnimeRelationUseCase(repository),
        stringProvider = stringProvider,
    )

    @Test
    fun `relation type follows the kind`() {
        assertEquals(RelationType.STUDIO, createViewModel(DetailsRelationKind.STUDIO).currentState.relationType)
        assertEquals(RelationType.DIRECTOR, createViewModel(DetailsRelationKind.DIRECTOR).currentState.relationType)
        assertEquals(RelationType.GENRE, createViewModel(DetailsRelationKind.GENRE).currentState.relationType)
    }

    @Test
    fun `loads the relation with the matching reference`() {
        val reference = slot<AnimeRelationReference>()
        coEvery { repository.getAnimeRelation(capture(reference)) } returns relation

        val state = createViewModel(DetailsRelationKind.DIRECTOR).currentState

        assertFalse(state.isLoading)
        assertEquals(relation, state.relation)
        assertEquals(AnimeRelationKind.DIRECTOR, reference.captured.kind)
        assertEquals(RELATION_ID, reference.captured.id)
        assertEquals("url", reference.captured.url)
    }

    @Test
    fun `failed load shows the parsed error and retry recovers`() {
        coEvery { repository.getAnimeRelation(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        assertEquals("parsed message", vm.currentState.error)
        coEvery { repository.getAnimeRelation(any()) } returns relation

        vm.setEvent(Event.RetrySelected)

        assertNull(vm.currentState.error)
        assertEquals(relation, vm.currentState.relation)
    }

    @Test
    fun `anime and sub genre selections navigate`() {
        val vm = createViewModel()

        vm.setEvent(Event.AnimeSelected(8))
        vm.setEvent(Event.SubGenreSelected(9))
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { detailsNavigator.getDetailsDest(8) }
        verify(exactly = 1) { detailsNavigator.getRelationDest(DetailsRelationKind.GENRE, 9, null) }
        verify(exactly = 2) { nav.navigate(navKey) }
        verify(exactly = 1) { nav.back() }
    }

    private companion object {
        const val RELATION_ID = 6
    }
}
