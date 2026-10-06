package su.afk.yummy.tv.feature.collection.catalog

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
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.error.api.StringProvider
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.testing.BaseUnitTest
import su.afk.yummy.tv.core.testing.collectEmissions
import su.afk.yummy.tv.domain.account.model.AccountSession
import su.afk.yummy.tv.domain.account.repository.AccountRepository
import su.afk.yummy.tv.domain.account.usecase.GetAccountSessionUseCase
import su.afk.yummy.tv.domain.collection.model.CreateCollectionRequest
import su.afk.yummy.tv.domain.collection.repository.CollectionMutationRepository
import su.afk.yummy.tv.domain.collection.repository.CollectionRepository
import su.afk.yummy.tv.domain.collection.usecase.CreateCollectionUseCase
import su.afk.yummy.tv.domain.collection.usecase.GetCollectionsUseCase
import su.afk.yummy.tv.feature.collection.ICollectionNavigator
import su.afk.yummy.tv.feature.collection.catalog.CollectionsCatalogState.Effect
import su.afk.yummy.tv.feature.collection.catalog.CollectionsCatalogState.Event

class CollectionsCatalogViewModelTest : BaseUnitTest() {

    private val errorHandler: ErrorHandler = mockk(relaxed = true)
    private val retryStorage: RetryStorage = mockk(relaxed = true)
    private val nav: INavigationManager = mockk(relaxed = true)
    private val collectionNavigator: ICollectionNavigator = mockk()
    private val repository: CollectionRepository = mockk()
    private val mutationNotifier: CollectionMutationRepository = mockk(relaxed = true)
    private val accountRepository: AccountRepository = mockk()
    private val stringProvider: StringProvider = mockk()
    private val collectionKey: NavKey = mockk()

    @Before
    fun setUp() {
        every { mutationNotifier.version } returns MutableStateFlow(0L)
        every { stringProvider.get(any<Int>()) } answers { "res${firstArg<Int>()}" }
        every { collectionNavigator.getCollectionDest(any()) } returns collectionKey
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = true, userId = 1)
    }

    private fun createViewModel() = CollectionsCatalogViewModel(
        errorHandler = errorHandler,
        retryStorage = retryStorage,
        nav = nav,
        collectionNavigator = collectionNavigator,
        getCollections = GetCollectionsUseCase(repository),
        mutationNotifier = mutationNotifier,
        createCollection = CreateCollectionUseCase(repository, mutationNotifier),
        getAccountSession = GetAccountSessionUseCase(accountRepository),
        stringProvider = stringProvider,
    )

    @Test
    fun `starts with a closed create dialog`() {
        val state = createViewModel().currentState

        assertFalse(state.isCreateDialogVisible)
        assertTrue(state.isCreatePublic)
    }

    @Test
    fun `authorized user opens an empty create dialog`() {
        val vm = createViewModel()

        vm.setEvent(Event.CreateSelected)

        assertTrue(vm.currentState.isCreateDialogVisible)
        assertEquals("", vm.currentState.createTitle)
    }

    @Test
    fun `guest gets a toast instead of the create dialog`() = runTest {
        coEvery { accountRepository.getSession() } returns AccountSession(isAuthorized = false, userId = 0)
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)

        vm.setEvent(Event.CreateSelected)

        assertFalse(vm.currentState.isCreateDialogVisible)
        assertEquals(1, effects.size)
        assertTrue(effects.single() is Effect.ShowToast)
    }

    @Test
    fun `form fields follow the events`() {
        val vm = createViewModel()

        vm.setEvent(Event.CreateTitleChanged("title"))
        vm.setEvent(Event.CreateDescriptionChanged("description"))
        vm.setEvent(Event.CreatePublicChanged(false))

        assertEquals("title", vm.currentState.createTitle)
        assertEquals("description", vm.currentState.createDescription)
        assertFalse(vm.currentState.isCreatePublic)
    }

    @Test
    fun `blank title is not created`() {
        val vm = createViewModel()
        vm.setEvent(Event.CreateTitleChanged("   "))

        vm.setEvent(Event.CreateConfirmed)

        coVerify(exactly = 0) { repository.createCollection(any()) }
    }

    @Test
    fun `confirmed form creates the collection and opens it`() {
        coEvery { repository.createCollection(any()) } returns 42
        val vm = createViewModel()
        vm.setEvent(Event.CreateSelected)
        vm.setEvent(Event.CreateTitleChanged(" Title "))
        vm.setEvent(Event.CreateDescriptionChanged(" About "))

        vm.setEvent(Event.CreateConfirmed)

        coVerify(exactly = 1) { repository.createCollection(CreateCollectionRequest("Title", "About", true)) }
        verify(exactly = 1) { mutationNotifier.notifyChanged() }
        verify(exactly = 1) { collectionNavigator.getCollectionDest(42) }
        verify(exactly = 1) { nav.navigate(collectionKey) }
        assertFalse(vm.currentState.isCreateDialogVisible)
        assertFalse(vm.currentState.isCreating)
    }

    @Test
    fun `failed creation keeps the dialog and shows a toast`() = runTest {
        coEvery { repository.createCollection(any()) } throws IllegalStateException("boom")
        val vm = createViewModel()
        val effects = collectEmissions(vm.effect)
        vm.setEvent(Event.CreateSelected)
        vm.setEvent(Event.CreateTitleChanged("Title"))

        vm.setEvent(Event.CreateConfirmed)

        assertTrue(vm.currentState.isCreateDialogVisible)
        assertFalse(vm.currentState.isCreating)
        assertEquals(1, effects.size)
        verify(exactly = 0) { nav.navigate(any()) }
    }

    @Test
    fun `dismiss closes the dialog`() {
        val vm = createViewModel()
        vm.setEvent(Event.CreateSelected)

        vm.setEvent(Event.CreateDismissed)

        assertFalse(vm.currentState.isCreateDialogVisible)
    }

    @Test
    fun `collection selection opens the collection and back leaves`() {
        val vm = createViewModel()

        vm.setEvent(Event.CollectionSelected(5))
        vm.setEvent(Event.BackSelected)

        verify(exactly = 1) { collectionNavigator.getCollectionDest(5) }
        verify(exactly = 1) { nav.navigate(collectionKey) }
        verify(exactly = 1) { nav.back() }
    }
}
