package su.afk.yummy.tv.feature.schedule.view

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.Job
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.feature.schedule.ScheduleState
import su.afk.yummy.tv.feature.schedule.model.ScheduleTimelineUi
import su.afk.yummy.tv.feature.schedule.utils.launchRestoreTimelineFocus

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ScheduleTimeline(
    schedule: ScheduleTimelineUi,
    onEvent: (ScheduleState.Event) -> Unit,
) {
    var contentHasFocus by remember { mutableStateOf(false) }
    val selectedGroup = schedule.selectedGroup ?: return
    val selectedChipFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val releaseFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var restoreFocusRequestToken by remember { mutableIntStateOf(0) }
    var handledRestoreFocusRequestToken by remember { mutableIntStateOf(0) }
    var restoreFocusJob by remember { mutableStateOf<Job?>(null) }
    val preferredContentFocusRequester =
        schedule.focusedReleaseKey?.let { releaseFocusRequesters[it] } ?: selectedChipFocusRequester

    DisposableEffect(Unit) {
        onDispose { restoreFocusJob?.cancel() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                restoreFocusRequestToken += 1
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(
        schedule.focusedReleaseKey,
        schedule.selectedEpochDay,
        selectedGroup.items,
        contentHasFocus,
        restoreFocusRequestToken,
    ) {
        val hasPendingResumeRestore = restoreFocusRequestToken != handledRestoreFocusRequestToken
        if (contentHasFocus || hasPendingResumeRestore) {
            if (hasPendingResumeRestore) {
                handledRestoreFocusRequestToken = restoreFocusRequestToken
            }
            restoreFocusJob = launchRestoreTimelineFocus(
                previousJob = restoreFocusJob,
                scope = scope,
                focusedReleaseKey = schedule.focusedReleaseKey,
                selectedGroup = selectedGroup,
                listState = listState,
                releaseFocusRequesters = releaseFocusRequesters,
                fallbackFocusRequester = selectedChipFocusRequester,
            )
        }
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .onFocusChanged { contentHasFocus = it.hasFocus }
            .focusProperties {
                onEnter = {
                    restoreFocusJob = launchRestoreTimelineFocus(
                        previousJob = restoreFocusJob,
                        scope = scope,
                        focusedReleaseKey = schedule.focusedReleaseKey,
                        selectedGroup = selectedGroup,
                        listState = listState,
                        releaseFocusRequesters = releaseFocusRequesters,
                        fallbackFocusRequester = selectedChipFocusRequester,
                    )
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
        ScheduleDateChips(
            groups = schedule.dayGroups,
            selectedEpochDay = schedule.selectedEpochDay,
            selectedFocusRequester = selectedChipFocusRequester,
            downFocusRequester = listFocusRequester,
            leftFocusRequester = mainMenuFocusRequester,
            onSelected = { onEvent(ScheduleState.Event.DateSelected(it)) },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(listFocusRequester)
                .focusProperties {
                    mainMenuFocusRequester?.let { left = it }
                    up = selectedChipFocusRequester
                    onEnter = {
                        restoreFocusJob = launchRestoreTimelineFocus(
                            previousJob = restoreFocusJob,
                            scope = scope,
                            focusedReleaseKey = schedule.focusedReleaseKey,
                            selectedGroup = selectedGroup,
                            listState = listState,
                            releaseFocusRequesters = releaseFocusRequesters,
                            fallbackFocusRequester = selectedChipFocusRequester,
                        )
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
                val releaseFocusRequester = remember(releaseKey) { FocusRequester() }
                DisposableEffect(releaseKey, releaseFocusRequester) {
                    releaseFocusRequesters[releaseKey] = releaseFocusRequester
                    onDispose {
                        if (releaseFocusRequesters[releaseKey] == releaseFocusRequester) {
                            releaseFocusRequesters.remove(releaseKey)
                        }
                    }
                }
                ScheduleReleaseRow(
                    release = release,
                    now = schedule.now,
                    zone = schedule.zone,
                    focusRequester = releaseFocusRequester,
                    leftFocusRequester = mainMenuFocusRequester,
                    upFocusRequester = if (index == 0) selectedChipFocusRequester else null,
                    onFocused = {
                        onEvent(
                            ScheduleState.Event.ReleaseFocused(
                                releaseKey = releaseKey,
                                epochDay = selectedGroup.date.toEpochDay(),
                            )
                        )
                    },
                    onClick = {
                        onEvent(
                            ScheduleState.Event.ReleaseFocused(
                                releaseKey = releaseKey,
                                epochDay = selectedGroup.date.toEpochDay(),
                            )
                        )
                        onEvent(ScheduleState.Event.AnimeSelected(release.item.animeId))
                    },
                )
            }
        }
    }
}
