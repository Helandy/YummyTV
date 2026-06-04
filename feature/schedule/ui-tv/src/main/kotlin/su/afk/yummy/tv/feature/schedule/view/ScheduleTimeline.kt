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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalMainMenuFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.domain.schedule.model.AnimeScheduleDay
import su.afk.yummy.tv.feature.schedule.ScheduleState
import su.afk.yummy.tv.feature.schedule.utils.toUiDayGroups
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
internal fun ScheduleTimeline(
    days: List<AnimeScheduleDay>,
    onEvent: (ScheduleState.Event) -> Unit,
) {
    val zone = remember { ZoneId.systemDefault() }
    val now = remember { ZonedDateTime.now(zone) }
    val today = now.toLocalDate()
    val dayGroups = remember(days, zone, today) { days.toUiDayGroups(zone, today) }
    val availableEpochDays = remember(dayGroups) { dayGroups.map { it.date.toEpochDay() } }
    val initialEpochDay = remember(availableEpochDays, now) {
        val todayEpochDay = today.toEpochDay()
        availableEpochDays.firstOrNull { it == todayEpochDay }
            ?: availableEpochDays.firstOrNull { it > todayEpochDay }
            ?: availableEpochDays.first()
    }
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(initialEpochDay) }
    var focusedReleaseKey by rememberSaveable { mutableStateOf<String?>(null) }
    var focusedReleaseEpochDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var contentHasFocus by remember { mutableStateOf(false) }
    val selectedGroup = dayGroups.firstOrNull { it.date.toEpochDay() == selectedEpochDay } ?: dayGroups.first()
    val selectedChipFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val releaseFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val mainMenuFocusRequester = LocalMainMenuFocusRequester.current
    val preferredContentFocusRequester =
        focusedReleaseKey?.let { releaseFocusRequesters[it] } ?: selectedChipFocusRequester

    LaunchedEffect(availableEpochDays, initialEpochDay, focusedReleaseKey, focusedReleaseEpochDay) {
        val restoredEpochDay = focusedReleaseEpochDay
            ?.takeIf { focusedReleaseKey != null && it in availableEpochDays }
        when {
            restoredEpochDay != null -> selectedEpochDay = restoredEpochDay
            selectedEpochDay !in availableEpochDays -> selectedEpochDay = initialEpochDay
        }
    }

    LaunchedEffect(focusedReleaseKey, selectedEpochDay, selectedGroup.items, contentHasFocus) {
        val releaseKey = focusedReleaseKey ?: return@LaunchedEffect
        val releaseIndex = selectedGroup.items.indexOfFirst { it.focusKey == releaseKey }
        if (releaseIndex < 0) return@LaunchedEffect

        listState.scrollToItem(releaseIndex)
        delay(50)
        if (contentHasFocus) {
            runCatching { releaseFocusRequesters[releaseKey]?.requestFocus() }
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
            groups = dayGroups,
            selectedEpochDay = selectedEpochDay,
            selectedFocusRequester = selectedChipFocusRequester,
            downFocusRequester = listFocusRequester,
            leftFocusRequester = mainMenuFocusRequester,
            onSelected = { selectedEpochDay = it },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(listFocusRequester)
                .focusProperties {
                    mainMenuFocusRequester?.let { left = it }
                    up = selectedChipFocusRequester
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
                    now = now,
                    zone = zone,
                    focusRequester = releaseFocusRequester,
                    leftFocusRequester = mainMenuFocusRequester,
                    upFocusRequester = if (index == 0) selectedChipFocusRequester else null,
                    onFocused = {
                        focusedReleaseKey = releaseKey
                        focusedReleaseEpochDay = selectedGroup.date.toEpochDay()
                    },
                    onClick = {
                        focusedReleaseKey = releaseKey
                        focusedReleaseEpochDay = selectedGroup.date.toEpochDay()
                        onEvent(ScheduleState.Event.AnimeSelected(release.item.animeId))
                    },
                )
            }
        }
    }
}
