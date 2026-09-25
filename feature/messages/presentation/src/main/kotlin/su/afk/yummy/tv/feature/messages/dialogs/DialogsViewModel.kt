package su.afk.yummy.tv.feature.messages.dialogs

import androidx.paging.insertHeaderItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import su.afk.yummy.tv.core.error.api.ErrorHandler
import su.afk.yummy.tv.core.error.api.RetryStorage
import su.afk.yummy.tv.core.mvi.BaseViewModel
import su.afk.yummy.tv.core.navigation.manager.INavigationManager
import su.afk.yummy.tv.core.utils.paging.PagedSource
import su.afk.yummy.tv.core.utils.paging.pagingSource
import su.afk.yummy.tv.domain.account.usecase.ObserveAccountSessionUseCase
import su.afk.yummy.tv.domain.messages.MessagesMutationNotifier
import su.afk.yummy.tv.domain.messages.model.DialogSummary
import su.afk.yummy.tv.domain.messages.model.GLOBAL_CHAT_USER_ID
import su.afk.yummy.tv.domain.messages.usecase.GetDialogsUseCase
import su.afk.yummy.tv.feature.account.IAccountNavigator
import su.afk.yummy.tv.feature.messages.IMessagesNavigator
import javax.inject.Inject

private const val DIALOGS_PAGE_SIZE = 20

@HiltViewModel
class DialogsViewModel @Inject constructor(
    override val errorHandler: ErrorHandler,
    override val retryStorage: RetryStorage,
    private val nav: INavigationManager,
    private val navigator: IMessagesNavigator,
    private val accountNavigator: IAccountNavigator,
    private val observeAccountSession: ObserveAccountSessionUseCase,
    private val getDialogs: GetDialogsUseCase,
    mutationNotifier: MessagesMutationNotifier,
) : BaseViewModel<DialogsState.State, DialogsState.Event, DialogsState.Effect>() {
    private var pagedSource: PagedSource<DialogSummary>? = null

    override fun createInitialState() = DialogsState.State()

    init {
        observeAccountSession()
            .onEach { session ->
                val shouldCreateFlow = session.isAuthorized && !currentState.isAuthorized
                setState {
                    copy(
                        isAuthResolved = true,
                        isAuthorized = session.isAuthorized,
                        dialogs = if (shouldCreateFlow) createDialogsFlow() else dialogs,
                    )
                }
            }
            .launchIn(viewModelScope)
        mutationNotifier.version
            .drop(1)
            .onEach { pagedSource?.invalidate() }
            .launchIn(viewModelScope)
    }

    override fun onEvent(event: DialogsState.Event) {
        when (event) {
            DialogsState.Event.BackSelected -> nav.back()
            DialogsState.Event.LoginSelected -> nav.navigate(accountNavigator.getAccountDest())
            is DialogsState.Event.DialogSelected -> if (event.userId >= 0) {
                nav.navigateDetail(navigator.chat(event.userId))
            }
        }
    }

    private fun createDialogsFlow() =
        pagingSource(viewModelScope, pageSize = DIALOGS_PAGE_SIZE, itemKey = { it.userId }) { limit, offset ->
            // Общий чат приклеивается заголовком ниже — из страниц его убираем, иначе в списке
            // окажутся два элемента с одним ключом.
            getDialogs(limit, offset).filterNot { it.userId == GLOBAL_CHAT_USER_ID }
        }.also { pagedSource = it }.flow
            // Общий чат сервер не отдаёт в списке диалогов — закрепляем его сверху сами.
            .map { it.insertHeaderItem(item = GLOBAL_CHAT_SUMMARY) }
}

/** Синтетическая запись общего чата. Имя/подпись подставляет UI по [GLOBAL_CHAT_USER_ID]. */
private val GLOBAL_CHAT_SUMMARY = DialogSummary(
    userId = GLOBAL_CHAT_USER_ID,
    nickname = "",
    avatarUrl = null,
    roles = emptyList(),
    isBanned = false,
    lastMessage = "",
    unreadCount = 0,
    dateSeconds = 0,
    lastOnlineSeconds = 0,
)
