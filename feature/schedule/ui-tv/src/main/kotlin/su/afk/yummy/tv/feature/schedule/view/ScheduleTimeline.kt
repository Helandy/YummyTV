package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.focus.launchTvLazyListKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.focus.requestFocusUntilTimeout
import su.afk.yummy.tv.core.designsystem.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.tv.TvStateMessage
import su.afk.yummy.tv.feature.schedule.ScheduleState
import su.afk.yummy.tv.feature.schedule.model.ScheduleTimelineUi
import su.afk.yummy.tv.feature.schedule.presentation.R

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ScheduleTimeline(
    schedule: ScheduleTimelineUi,
    onlyInLibrary: Boolean,
    requestInitialFocus: Boolean,
    onInitialFocusHandled: () -> Unit,
    onEvent: (ScheduleState.Event) -> Unit,
) {
    val selectedGroup = schedule.selectedGroup
    val timelineEntryFocusRequester = remember { FocusRequester() }
    val filterChipFocusRequester = remember { FocusRequester() }
    val emptyResetFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dayKeys = schedule.dayGroups.map { it.date.toEpochDay() }
    val dateChipFocusRequesters = remember(dayKeys) {
        dayKeys.associateWith { FocusRequester() }
    }
    val selectedChipFocusRequester = schedule.selectedEpochDay
        ?.let(dateChipFocusRequesters::get)
        ?: filterChipFocusRequester
    val releaseKeys = selectedGroup?.items.orEmpty().map { it.focusKey }
    val focusRestoreState = rememberTvLazyFocusRestoreState<String>(schedule.selectedEpochDay, onlyInLibrary)
    val releaseFocusRequesters = remember(schedule.selectedEpochDay, releaseKeys) {
        releaseKeys.associateWith { FocusRequester() }
    }
    val firstReleaseFocusRequester = releaseKeys.firstOrNull()?.let(releaseFocusRequesters::get)
    val contentDownFocusRequester = when {
        firstReleaseFocusRequester != null -> firstReleaseFocusRequester
        selectedGroup == null -> emptyResetFocusRequester
        else -> selectedChipFocusRequester
    }
    val savedReleaseFocusRequester = focusRestoreState.savedKey?.let(releaseFocusRequesters::get)
    val releaseRestoreFocusRequester = savedReleaseFocusRequester ?: contentDownFocusRequester
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var restoreFocusRequestToken by remember { mutableIntStateOf(0) }
    var handledRestoreFocusRequestToken by remember { mutableIntStateOf(0) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    var pendingRestoreFocusKey by remember(schedule.selectedEpochDay) { mutableStateOf<String?>(null) }
    var focusFilterOnReset by remember { mutableStateOf(false) }

    fun launchReleaseFocusRestore(): Job {
        pendingRestoreFocusKey = focusRestoreState.savedKey
        return launchTvLazyListKeyFocusRestore(
            previousJob = restoreFocusJob,
            scope = scope,
            restoreState = focusRestoreState,
            keys = releaseKeys,
            listState = listState,
            itemFocusRequesters = releaseFocusRequesters,
            fallbackFocusRequester = releaseRestoreFocusRequester,
            onRestoreFinished = { pendingRestoreFocusKey = null },
        )
    }

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                pendingRestoreFocusKey = focusRestoreState.savedKey
                restoreFocusRequestToken += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        schedule.selectedEpochDay,
        releaseKeys,
        restoreFocusRequestToken,
    ) {
        val hasPendingResumeRestore = restoreFocusRequestToken != handledRestoreFocusRequestToken
        if (hasPendingResumeRestore) {
            handledRestoreFocusRequestToken = restoreFocusRequestToken
            if (selectedGroup == null || focusRestoreState.savedKey == null) {
                runCatching { selectedChipFocusRequester.requestFocus() }
            } else {
                restoreFocusJob = launchReleaseFocusRestore()
            }
        }
    }

    LaunchedEffect(onlyInLibrary, selectedGroup) {
        if (focusFilterOnReset && !onlyInLibrary && selectedGroup != null) {
            focusFilterOnReset = false
            requestFocusUntilTimeout(selectedChipFocusRequester)
        }
    }

    LaunchedEffect(requestInitialFocus, selectedChipFocusRequester) {
        if (!requestInitialFocus) return@LaunchedEffect
        requestFocusUntilTimeout(selectedChipFocusRequester)
        onInitialFocusHandled()
    }

    val preferredContentFocusRequester = if (selectedGroup == null || focusRestoreState.savedKey == null) {
        timelineEntryFocusRequester
    } else {
        listFocusRequester
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose {
            registerPreferredContentFocusRequester?.invoke(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .focusProperties {
                onEnter = {
                    if (selectedGroup == null || focusRestoreState.savedKey == null) {
                        runCatching { selectedChipFocusRequester.requestFocus() }
                    } else {
                        restoreFocusJob = launchReleaseFocusRestore()
                    }
                }
            }
            .focusGroup()
            .padding(
                start = TvScreenPadding.Horizontal,
                end = TvScreenPadding.Horizontal,
                top = 18.dp,
                bottom = TvScreenPadding.Vertical,
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(timelineEntryFocusRequester)
                .focusProperties {
                    down = contentDownFocusRequester
                    mainMenuFocusRequester?.let { left = it }
                    onEnter = {
                        runCatching { selectedChipFocusRequester.requestFocus() }
                    }
                }
                .focusGroup(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScheduleLibraryFilterChip(
                selected = onlyInLibrary,
                focusRequester = filterChipFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
                downFocusRequester = contentDownFocusRequester,
                onClick = { onEvent(ScheduleState.Event.InLibraryFilterToggled) },
            )
            if (schedule.dayGroups.isNotEmpty()) {
                ScheduleDateChips(
                    groups = schedule.dayGroups,
                    selectedEpochDay = schedule.selectedEpochDay,
                    chipFocusRequesters = dateChipFocusRequesters,
                    downFocusRequester = contentDownFocusRequester,
                    leftFocusRequester = filterChipFocusRequester,
                    onSelected = { onEvent(ScheduleState.Event.DateSelected(it)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (selectedGroup == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .focusProperties {
                        mainMenuFocusRequester?.let { left = it }
                        up = filterChipFocusRequester
                    },
                contentAlignment = Alignment.Center,
            ) {
                TvStateMessage(
                    title = stringResource(R.string.schedule_filter_in_library_empty),
                    icon = Icons.Filled.DateRange,
                    retryLabel = stringResource(R.string.schedule_filter_show_all),
                    onRetry = {
                        focusFilterOnReset = true
                        onEvent(ScheduleState.Event.InLibraryFilterToggled)
                    },
                    retryFocusRequester = emptyResetFocusRequester,
                    fillMaxSize = false,
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .focusRequester(listFocusRequester)
                    .tvFocusRestorer(
                        fallback = releaseRestoreFocusRequester,
                        enabled = selectedGroup.items.isNotEmpty(),
                    )
                    .focusProperties {
                        mainMenuFocusRequester?.let { left = it }
                        up = selectedChipFocusRequester
                        onEnter = {
                            restoreFocusJob = launchReleaseFocusRestore()
                        }
                    },
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                itemsIndexed(
                    selectedGroup.items,
                    key = { _, release -> release.focusKey },
                ) { index, release ->
                    val releaseKey = release.focusKey
                    val releaseFocusRequester = releaseFocusRequesters.getValue(releaseKey)
                    ScheduleReleaseRow(
                        release = release,
                        now = schedule.now,
                        zone = schedule.zone,
                        focusRequester = releaseFocusRequester,
                        leftFocusRequester = mainMenuFocusRequester,
                        upFocusRequester = if (index == 0) selectedChipFocusRequester else null,
                        selected = releaseKey == focusRestoreState.savedKey,
                        onFocused = {
                            val restoreKey = pendingRestoreFocusKey
                            if (restoreKey == null || restoreKey == releaseKey) {
                                focusRestoreState.onItemFocused(releaseKey, index)
                                if (restoreKey == releaseKey) {
                                    pendingRestoreFocusKey = null
                                }
                            }
                        },
                        onClick = {
                            focusRestoreState.onItemFocused(releaseKey, index)
                            onEvent(ScheduleState.Event.AnimeSelected(release.item.animeId))
                        },
                    )
                }
            }
        }
    }
}
