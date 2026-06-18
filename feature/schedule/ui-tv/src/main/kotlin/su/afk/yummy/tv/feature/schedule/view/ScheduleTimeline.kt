package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.launchTvLazyListKeyFocusRestore
import su.afk.yummy.tv.core.designsystem.presenter.focus.rememberTvLazyFocusRestoreState
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusRestorer
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.feature.schedule.ScheduleState
import su.afk.yummy.tv.feature.schedule.model.ScheduleTimelineUi

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ScheduleTimeline(
    schedule: ScheduleTimelineUi,
    onEvent: (ScheduleState.Event) -> Unit,
) {
    val selectedGroup = schedule.selectedGroup ?: return
    val timelineEntryFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val dayKeys = schedule.dayGroups.map { it.date.toEpochDay() }
    val dateChipFocusRequesters = remember(dayKeys) {
        dayKeys.associateWith { FocusRequester() }
    }
    val fallbackDateFocusRequester = remember { FocusRequester() }
    val selectedChipFocusRequester = schedule.selectedEpochDay
        ?.let(dateChipFocusRequesters::get)
        ?: fallbackDateFocusRequester
    val releaseKeys = selectedGroup.items.map { it.focusKey }
    val focusRestoreState = rememberTvLazyFocusRestoreState<String>(schedule.selectedEpochDay)
    val releaseFocusRequesters = remember(schedule.selectedEpochDay, releaseKeys) {
        releaseKeys.associateWith { FocusRequester() }
    }
    val firstReleaseFocusRequester = releaseKeys.firstOrNull()?.let(releaseFocusRequesters::get)
    val releaseEntryFocusRequester = firstReleaseFocusRequester ?: selectedChipFocusRequester
    val savedReleaseFocusRequester = focusRestoreState.savedKey?.let(releaseFocusRequesters::get)
    val releaseRestoreFocusRequester = savedReleaseFocusRequester ?: releaseEntryFocusRequester
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var restoreFocusRequestToken by remember { mutableIntStateOf(0) }
    var handledRestoreFocusRequestToken by remember { mutableIntStateOf(0) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    var pendingRestoreFocusKey by remember(schedule.selectedEpochDay) { mutableStateOf<String?>(null) }

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
            if (focusRestoreState.savedKey == null) {
                runCatching { selectedChipFocusRequester.requestFocus() }
            } else {
                restoreFocusJob = launchReleaseFocusRestore()
            }
        }
    }

    val preferredContentFocusRequester = if (focusRestoreState.savedKey == null) {
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
                    if (focusRestoreState.savedKey == null) {
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
        Box(
            modifier = Modifier
                .focusRequester(timelineEntryFocusRequester)
                .focusProperties {
                    down = releaseEntryFocusRequester
                    mainMenuFocusRequester?.let { left = it }
                    onEnter = {
                        runCatching { selectedChipFocusRequester.requestFocus() }
                    }
                }
                .focusable(),
        ) {
            ScheduleDateChips(
                groups = schedule.dayGroups,
                selectedEpochDay = schedule.selectedEpochDay,
                chipFocusRequesters = dateChipFocusRequesters,
                downFocusRequester = releaseEntryFocusRequester,
                leftFocusRequester = mainMenuFocusRequester,
                onSelected = { onEvent(ScheduleState.Event.DateSelected(it)) },
            )
        }

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
