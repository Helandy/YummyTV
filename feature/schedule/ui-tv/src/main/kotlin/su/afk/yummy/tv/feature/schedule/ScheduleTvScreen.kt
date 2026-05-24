package su.afk.yummy.tv.feature.schedule

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import su.afk.yummy.tv.core.designsystem.presenter.dimensions.TvScreenPadding
import su.afk.yummy.tv.core.designsystem.presenter.focus.tvFocusableClick
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalPreferredContentFocusRequester
import su.afk.yummy.tv.core.designsystem.presenter.locals.LocalTopBarFocusRequester
import su.afk.yummy.tv.domain.schedule.AnimeScheduleDay
import su.afk.yummy.tv.domain.schedule.AnimeScheduleItem
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.TextStyle
import java.util.Locale

private val AiredColor = Color(0xFF4CAF50)

@Composable
fun ScheduleTvScreen(
    state: ScheduleState.State,
    effect: Flow<ScheduleState.Effect>,
    onEvent: (ScheduleState.Event) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        when {
            state.isLoading -> ScheduleLoadingContent()
            state.error != null -> Text(
                text = state.error.orEmpty(),
                color = MaterialTheme.colorScheme.error,
            )
            state.days.isEmpty() -> Text(
                text = stringResource(R.string.schedule_empty),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> ScheduleContent(state.days, onEvent)
        }
    }
}

@Composable
private fun ScheduleContent(
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
    val selectedGroup = dayGroups.firstOrNull { it.date.toEpochDay() == selectedEpochDay } ?: dayGroups.first()
    val selectedChipFocusRequester = remember { FocusRequester() }
    val listFocusRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val releaseFocusRequesters = remember { mutableStateMapOf<String, FocusRequester>() }
    val registerPreferredContentFocusRequester = LocalPreferredContentFocusRequester.current
    val topBarFocusRequester = LocalTopBarFocusRequester.current
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

    LaunchedEffect(focusedReleaseKey, selectedEpochDay, selectedGroup.items) {
        val releaseKey = focusedReleaseKey ?: return@LaunchedEffect
        val releaseIndex = selectedGroup.items.indexOfFirst { it.focusKey == releaseKey }
        if (releaseIndex < 0) return@LaunchedEffect

        listState.scrollToItem(releaseIndex)
        delay(50)
        runCatching { releaseFocusRequesters[releaseKey]?.requestFocus() }
    }

    DisposableEffect(preferredContentFocusRequester, registerPreferredContentFocusRequester) {
        registerPreferredContentFocusRequester?.invoke(preferredContentFocusRequester)
        onDispose { registerPreferredContentFocusRequester?.invoke(null) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
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
            upFocusRequester = topBarFocusRequester,
            onSelected = { selectedEpochDay = it },
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(listFocusRequester)
                .focusProperties { up = selectedChipFocusRequester },
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

@Composable
private fun ScheduleDateChips(
    groups: List<ScheduleDayUi>,
    selectedEpochDay: Long,
    selectedFocusRequester: FocusRequester,
    downFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onSelected: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
    ) {
        itemsIndexed(groups, key = { _, group -> group.date.toEpochDay() }) { _, group ->
            val selected = group.date.toEpochDay() == selectedEpochDay
            ScheduleDateChip(
                group = group,
                selected = selected,
                focusRequester = if (selected) selectedFocusRequester else null,
                downFocusRequester = downFocusRequester,
                upFocusRequester = upFocusRequester,
                onSelected = { onSelected(group.date.toEpochDay()) },
            )
        }
    }
}

@Composable
private fun ScheduleDateChip(
    group: ScheduleDayUi,
    selected: Boolean,
    focusRequester: FocusRequester?,
    downFocusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onSelected: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(10.dp)
    val background = when {
        selected -> MaterialTheme.colorScheme.primary
        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        modifier = Modifier
            .width(78.dp)
            .height(72.dp)
            .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
            .focusProperties {
                down = downFocusRequester
                upFocusRequester?.let { up = it }
            }
            .onFocusChanged { if (it.isFocused) onSelected() }
            .clip(shape)
            .background(background, shape)
            .border(
                width = if (focused && !selected) 2.dp else 0.dp,
                color = if (focused && !selected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = shape,
            )
            .tvFocusableClick(onClick = onSelected, interactionSource = interactionSource, shape = shape)
            .padding(8.dp),
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = group.weekdayLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
            )
            Text(
                text = group.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = contentColor,
                maxLines = 1,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(22.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(
                    color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = group.items.size.toString(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
            )
        }
    }
}

@Composable
private fun ScheduleReleaseRow(
    release: ScheduleReleaseUi,
    now: ZonedDateTime,
    zone: ZoneId,
    focusRequester: FocusRequester,
    upFocusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    val item = release.item
    val releaseAt = Instant.ofEpochSecond(release.epochSeconds).atZone(zone)
    val aired = releaseAt.isAfter(now).not()
    val accentColor = if (aired) AiredColor else MaterialTheme.colorScheme.onSurfaceVariant
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shape = RoundedCornerShape(8.dp)
    val rowScale by animateFloatAsState(
        targetValue = if (focused) 1.025f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "schedule_row_scale",
    )
    val rowBackground = when {
        focused -> MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        aired -> AiredColor.copy(alpha = 0.10f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.045f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .zIndex(if (focused) 1f else 0f)
            .focusRequester(focusRequester)
            .focusProperties {
                upFocusRequester?.let { up = it }
            }
            .onFocusChanged { if (it.isFocused) onFocused() }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    scaleX = rowScale
                    scaleY = rowScale
                }
                .clip(shape)
                .background(rowBackground, shape)
                .border(
                    width = if (focused) 2.dp else 0.dp,
                    color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = shape,
                ),
        )
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)),
            ) {
                AsyncImage(
                    model = item.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (focused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (aired) {
                        stringResource(R.string.schedule_episode_aired, release.episode)
                    } else {
                        stringResource(R.string.schedule_episode_future, release.episode, releaseAt.remainingText(now))
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = releaseAt.timeLabel(),
                modifier = Modifier.widthIn(min = 82.dp),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ScheduleLoadingContent() {
    val transition = rememberInfiniteTransition(label = "schedule_loading")
    val alpha by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.86f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 850),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "schedule_loading_alpha",
    )
    val baseColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
    val brightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = TvScreenPadding.Horizontal,
                end = TvScreenPadding.Horizontal,
                top = 18.dp,
                bottom = TvScreenPadding.Vertical,
            ),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
        ) {
            repeat(7) { index ->
                ScheduleSkeletonBlock(
                    modifier = Modifier
                        .width(78.dp)
                        .height(72.dp),
                    alpha = alpha,
                    color = if (index == 0) brightColor else baseColor,
                    shape = RoundedCornerShape(10.dp),
                )
            }
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            repeat(6) {
                ScheduleSkeletonRow(alpha = alpha, color = baseColor)
            }
        }
    }
}

@Composable
private fun ScheduleSkeletonRow(alpha: Float, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScheduleSkeletonBlock(
            modifier = Modifier
                .width(74.dp)
                .fillMaxHeight(),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(6.dp),
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ScheduleSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.58f)
                    .height(22.dp),
                alpha = alpha,
                color = color,
                shape = RoundedCornerShape(5.dp),
            )
            ScheduleSkeletonBlock(
                modifier = Modifier
                    .fillMaxWidth(0.42f)
                    .height(18.dp),
                alpha = alpha,
                color = color,
                shape = RoundedCornerShape(5.dp),
            )
        }
        ScheduleSkeletonBlock(
            modifier = Modifier
                .width(82.dp)
                .height(28.dp),
            alpha = alpha,
            color = color,
            shape = RoundedCornerShape(5.dp),
        )
    }
}

@Composable
private fun ScheduleSkeletonBlock(
    modifier: Modifier,
    alpha: Float,
    color: Color,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = modifier
            .graphicsLayer { this.alpha = alpha }
            .clip(shape)
            .background(color, shape),
    )
}

private data class ScheduleDayUi(
    val date: LocalDate,
    val weekdayLabel: String,
    val items: List<ScheduleReleaseUi>,
)

private data class ScheduleReleaseUi(
    val item: AnimeScheduleItem,
    val epochSeconds: Long,
    val episode: Int,
    val aired: Boolean,
) {
    val focusKey: String = "${item.animeId}:$epochSeconds:$aired"
}

private fun List<AnimeScheduleDay>.toUiDayGroups(
    zone: ZoneId,
    today: LocalDate,
): List<ScheduleDayUi> =
    flatMap { it.items }
        .flatMap { item ->
            buildList {
                item.previousDateEpochSeconds?.let { previousDate ->
                    add(
                        ScheduleReleaseUi(
                            item = item,
                            epochSeconds = previousDate,
                            episode = item.airedEpisodes ?: 0,
                            aired = true,
                        )
                    )
                }
                item.nextDateEpochSeconds?.let { nextDate ->
                    add(
                        ScheduleReleaseUi(
                            item = item,
                            epochSeconds = nextDate,
                            episode = (item.airedEpisodes ?: 0) + 1,
                            aired = false,
                        )
                    )
                }
            }
        }
        .filter { release ->
            Instant.ofEpochSecond(release.epochSeconds).atZone(zone).toLocalDate() >= today
        }
        .distinctBy { "${it.item.animeId}:${it.epochSeconds}:${it.aired}" }
        .sortedBy { it.epochSeconds }
        .groupBy { release ->
            Instant.ofEpochSecond(release.epochSeconds).atZone(zone).toLocalDate()
        }
        .toSortedMap()
        .map { (date, items) ->
            ScheduleDayUi(
                date = date,
                weekdayLabel = date.dayOfWeek
                    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
                    .trimEnd('.')
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                items = items.sortedBy { it.epochSeconds },
            )
        }

private fun ZonedDateTime.timeLabel(): String =
    "%02d:%02d".format(hour, minute)

private fun ZonedDateTime.remainingText(now: ZonedDateTime): String {
    val duration = Duration.between(now, this).coerceAtLeast(Duration.ZERO)
    val days = duration.toDays()
    val hours = duration.minusDays(days).toHours()
    val minutes = duration.minusDays(days).minusHours(hours).toMinutes()
    return when {
        days > 0 -> listOfNotNull(
            days.toInt().ruUnit("день", "дня", "дней"),
            hours.takeIf { it > 0 }?.toInt()?.ruUnit("час", "часа", "часов"),
        ).joinToString(" ")
        hours > 0 -> listOfNotNull(
            hours.toInt().ruUnit("час", "часа", "часов"),
            minutes.takeIf { it > 0 }?.toInt()?.ruUnit("минута", "минуты", "минут"),
        ).joinToString(" ")
        minutes > 0 -> minutes.toInt().ruUnit("минута", "минуты", "минут")
        else -> "меньше чем минуту"
    }
}

private fun Int.ruUnit(one: String, few: String, many: String): String {
    val mod100 = this % 100
    val mod10 = this % 10
    val unit = when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
    return "$this $unit"
}
