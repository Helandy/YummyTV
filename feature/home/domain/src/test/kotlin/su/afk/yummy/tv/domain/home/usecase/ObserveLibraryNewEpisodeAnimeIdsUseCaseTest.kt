package su.afk.yummy.tv.domain.home.usecase

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import su.afk.yummy.tv.core.model.settings.NewEpisodesSource
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.LibraryItem
import su.afk.yummy.tv.domain.library.repository.LibraryRepository
import su.afk.yummy.tv.domain.library.usecase.ObserveLibraryItemsUseCase

class ObserveLibraryNewEpisodeAnimeIdsUseCaseTest : BaseUnitTest() {

    private val repository: LibraryRepository = mockk()
    private val library = mutableListOf<LibraryItem>()

    @Before
    fun setUp() {
        every { repository.observeAll() } answers { flowOf(library.toList()) }
    }

    @Test
    fun `no sources means nothing to show`() = runTest {
        library += libraryItem(animeId = 1, listId = UserAnimeList.WATCHING.id)

        assertEquals(emptySet<Int>(), createUseCase()(emptySet()).first())
    }

    @Test
    fun `only titles from the selected lists are returned`() = runTest {
        library += libraryItem(animeId = 1, listId = UserAnimeList.WATCHING.id)
        library += libraryItem(animeId = 2, listId = UserAnimeList.PLANNED.id)
        library += libraryItem(animeId = 3, listId = UserAnimeList.DROPPED.id, isFavorite = true)

        val ids = createUseCase()(setOf(NewEpisodesSource.WATCHING, NewEpisodesSource.FAVORITES)).first()

        assertEquals(setOf(1, 3), ids)
    }

    @Test
    fun `a title matching two sources is returned once`() = runTest {
        library += libraryItem(animeId = 1, listId = UserAnimeList.WATCHING.id, isFavorite = true)

        val ids = createUseCase()(setOf(NewEpisodesSource.WATCHING, NewEpisodesSource.FAVORITES)).first()

        assertEquals(setOf(1), ids)
    }

    @Test
    fun `an empty library yields no ids`() = runTest {
        assertEquals(emptySet<Int>(), createUseCase()(NewEpisodesSource.DEFAULT).first())
    }

    private fun createUseCase() =
        ObserveLibraryNewEpisodeAnimeIdsUseCase(ObserveLibraryItemsUseCase(repository))

    private fun libraryItem(animeId: Int, listId: Int, isFavorite: Boolean = false) = LibraryItem(
        animeId = animeId,
        title = "Title $animeId",
        listId = listId,
        isFavorite = isFavorite,
    )
}
