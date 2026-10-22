package su.afk.yummy.tv.feature.main.view

import su.afk.yummy.tv.core.designsystem.seasonal.SeasonalOverlay
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.launch
import su.afk.yummy.tv.core.designsystem.components.GlobalToastOverlay
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.navigation.root.RootTab
import su.afk.yummy.tv.feature.main.MainState
import su.afk.yummy.tv.feature.main.model.TvMenuItem

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TvMainScaffold(
    selectedRoot: RootTab,
    contentFocusKey: Any?,
    menuItems: List<TvMenuItem>,
    state: MainState.State,
    showMainMenu: Boolean = true,
    applyTopSafeDrawingInset: Boolean = true,
    onEvent: (MainState.Event) -> Unit = {},
    toastMessage: String? = null,
    showSeasonalOverlay: Boolean = true,
    content: @Composable () -> Unit,
) {
    val focusController = rememberTvMainFocusController(showMainMenu)
    val selectedRootFocusRequester = focusController.selectedRootFocusRequester(selectedRoot)
    val currentPreferredContentFocusRequester =
        focusController.currentPreferredContentFocusRequester(selectedRoot, contentFocusKey)
    val registerPreferredContentFocusRequester = remember(
        focusController,
        contentFocusKey,
        selectedRoot,
    ) {
        focusController.preferredContentFocusRegistration(selectedRoot, contentFocusKey)
    }
    val accountLabel = if (state.isYaniSignedIn) state.yaniNickname else null
    val accountAvatarUrl = if (state.isYaniSignedIn) state.yaniAvatarUrl else ""
    var menuExpanded by remember { mutableStateOf(false) }
    val menuCanFocus = focusController.menuCanFocus &&
        focusController.previousShowMainMenu &&
        !focusController.restoreContentFocusAfterMenuShown &&
        focusController.pendingContentFocusRequest == null
    val topSafeDrawingModifier = if (applyTopSafeDrawingInset) {
        Modifier.windowInsetsPadding(
            WindowInsets.safeDrawing.only(WindowInsetsSides.Top),
        )
    } else {
        Modifier
    }

    LaunchedEffect(showMainMenu) {
        if (!showMainMenu) {
            menuExpanded = false
        }
    }

    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    // DOWN этого BACK не обработал ни один экран — значит, UP тоже наш (см. onKeyEvent ниже).
    var unhandledBackDown by remember { mutableStateOf(false) }
    val pendingContentFocus = focusController.pendingContentFocusRequest != null
    val backScope = rememberCoroutineScope()
    // pendingContentFocus покрывает окно nav-перехода: пока фокус контента не установлен,
    // BACK возвращает в меню вместо сворачивания приложения
    BackHandler(
        enabled = showMainMenu &&
            focusController.previousShowMainMenu &&
            !menuExpanded &&
            (pendingContentFocus || (menuCanFocus && focusController.contentHasFocus)),
    ) {
        // С predictive back (targetSdk 36+) BACK приходит в обход onPreviewKeyEvent: без отметки
        // о нажатии меню сочтёт фокус системным возвратом и сразу отдаст его контенту.
        focusController.onUserKey()
        focusController.cancelPendingContentFocusRequest()
        val focused = runCatching { selectedRootFocusRequester.requestFocus() }.getOrDefault(false)
        if (focused) {
            menuExpanded = true
        } else {
            // Меню становится фокусируемым только после рекомпозиции с отменённым запросом
            // фокуса контента — повторяем через кадр, иначе BACK молча проглатывается.
            backScope.launch {
                withFrameNanos { }
                focusController.onUserKey()
                if (runCatching { selectedRootFocusRequester.requestFocus() }.getOrDefault(false)) {
                    menuExpanded = true
                }
            }
        }
    }

    TvMainFocusEffects(
        focusController = focusController,
        showMainMenu = showMainMenu,
        selectedRoot = selectedRoot,
        contentFocusKey = contentFocusKey,
        currentPreferredContentFocusRequester = currentPreferredContentFocusRequester,
        selectedRootFocusRequester = selectedRootFocusRequester,
    )

    CompositionLocalProvider(
        LocalMainMenuFocusRequester provides selectedRootFocusRequester,
        LocalPreferredContentFocusRequester provides registerPreferredContentFocusRequester,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .focusProperties {
                    // Удалили узел с фокусом (лоадер сменился ошибкой или контентом) — Android
                    // возвращает фокус в Compose «на вход», и без этого он садится в первый по
                    // порядку элемент — боковое меню, которое тут же раскрывается. Отдаём фокус
                    // контенту; если там нечего фокусировать, вход идёт как обычно.
                    onEnter = {
                        if (showMainMenu && requestedFocusDirection == FocusDirection.Enter) {
                            runCatching { focusController.contentFocusRequester.requestFocus() }
                        }
                    }
                }
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown) focusController.onUserKey()
                    false
                }
                // Необработанный экранами BACK Compose превращает в FocusDirection.Exit: внутри
                // групп фокуса (кнопки карточек, сетки с восстановлением фокуса) «выход» съедает
                // нажатие, и activity не доходит до BackHandler — «Назад» молча не работает.
                // Забираем такой BACK здесь и отдаём диспетчеру на отпускание, как система.
                .onKeyEvent { event ->
                    if (event.key != Key.Back) return@onKeyEvent false
                    when (event.type) {
                        KeyEventType.KeyDown -> {
                            if (event.nativeKeyEvent.repeatCount == 0) unhandledBackDown = true
                            true
                        }

                        KeyEventType.KeyUp -> {
                            if (!unhandledBackDown) return@onKeyEvent false
                            unhandledBackDown = false
                            if (!event.nativeKeyEvent.isCanceled) backDispatcher?.onBackPressed()
                            true
                        }

                        else -> false
                    }
                }
                .focusGroup(),
        ) {
            Box(
                modifier = topSafeDrawingModifier.fillMaxSize(),
            ) {
                TvMainContentPane(
                    showMainMenu = showMainMenu,
                    menuExpanded = menuExpanded,
                    contentFocusRequester = focusController.contentFocusRequester,
                    selectedRootFocusRequester = selectedRootFocusRequester,
                    currentPreferredContentFocusRequester = currentPreferredContentFocusRequester,
                    onFocusChanged = { isFocused, hasFocus ->
                        focusController.onContentFocusChanged(
                            isFocused = isFocused,
                            hasFocus = hasFocus,
                            currentPreferredContentFocusRequester = currentPreferredContentFocusRequester,
                        )
                    },
                ) {
                    content()
                }
            }

            if (showSeasonalOverlay) SeasonalOverlay()
            GlobalToastOverlay(text = toastMessage)

            if (showMainMenu) {
                Box(modifier = topSafeDrawingModifier.fillMaxSize()) {
                    TvSideMenu(
                        selectedRoot = selectedRoot,
                        menuItems = menuItems,
                        accountLabel = accountLabel,
                        accountAvatarUrl = accountAvatarUrl,
                        unreadNotificationsCount = state.unreadNotificationsCount,
                        expanded = menuExpanded,
                        onExpandedChange = { expanded ->
                            // Пока запрос фокуса контента в полёте, меню не должно
                            // само раскрываться, перехватив временно потерянный фокус
                            if (expanded && !menuExpanded &&
                                !focusController.isMenuFocusUserInitiated()
                            ) {
                                // Фокус попал в меню без нажатия (системный возврат при смене
                                // экрана на старте): не раскрываем, отдаём фокус контенту
                                focusController.requestContentFocus(selectedRoot)
                            } else {
                                menuExpanded = expanded &&
                                    focusController.pendingContentFocusRequest == null
                            }
                        },
                        onEvent = onEvent,
                        rootFocusRequesters = focusController.rootFocusRequesters,
                        menuEnterFocusRequester = selectedRootFocusRequester,
                        rightFocusRequesterForRoot = focusController.rightFocusRequesterForRoot,
                        canFocus = menuCanFocus,
                        onMenuNavigationFocusLocked =
                        focusController::updateMenuNavigationFocusLocked,
                        onMoveToContent = { root ->
                            menuExpanded = false
                            focusController.requestContentFocus(root)
                        },
                    )
                }
            }
        }
    }
}
