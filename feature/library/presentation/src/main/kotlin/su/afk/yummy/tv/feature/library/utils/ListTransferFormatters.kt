package su.afk.yummy.tv.feature.library.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.domain.library.model.AnimeListImportPhase
import su.afk.yummy.tv.domain.library.model.AnimeListImportProgress
import su.afk.yummy.tv.domain.library.model.ExternalListFormat
import su.afk.yummy.tv.domain.library.model.ImportConflictPolicy
import su.afk.yummy.tv.feature.library.presentation.R

@Composable
fun ExternalListFormat.label(): String = stringResource(
    when (this) {
        ExternalListFormat.MAL_XML -> R.string.library_transfer_format_mal_xml
        ExternalListFormat.SHIKIMORI_JSON -> R.string.library_transfer_format_shikimori_json
    },
)

@Composable
fun ExternalListFormat.hint(): String = stringResource(
    when (this) {
        ExternalListFormat.MAL_XML -> R.string.library_transfer_format_mal_xml_hint
        ExternalListFormat.SHIKIMORI_JSON -> R.string.library_transfer_format_shikimori_json_hint
    },
)

@Composable
fun ImportConflictPolicy.label(): String = stringResource(
    when (this) {
        ImportConflictPolicy.SKIP_EXISTING -> R.string.library_transfer_conflict_skip
        ImportConflictPolicy.OVERWRITE -> R.string.library_transfer_conflict_overwrite
    },
)

@Composable
fun ImportConflictPolicy.hint(): String = stringResource(
    when (this) {
        ImportConflictPolicy.SKIP_EXISTING -> R.string.library_transfer_conflict_skip_hint
        ImportConflictPolicy.OVERWRITE -> R.string.library_transfer_conflict_overwrite_hint
    },
)

@Composable
fun UserAnimeList.transferLabel(): String = stringResource(
    when (this) {
        UserAnimeList.WATCHING -> R.string.library_tab_watching
        UserAnimeList.PLANNED -> R.string.library_tab_planned
        UserAnimeList.COMPLETED -> R.string.library_tab_completed
        UserAnimeList.POSTPONED -> R.string.library_tab_postponed
        UserAnimeList.DROPPED -> R.string.library_tab_dropped
    },
)

@Composable
fun AnimeListImportProgress.phaseLabel(): String =
    when (phase) {
        AnimeListImportPhase.RESOLVING_TITLES ->
            stringResource(R.string.library_transfer_phase_resolving, processed, total)
        AnimeListImportPhase.LOADING_CURRENT_LIST ->
            stringResource(R.string.library_transfer_phase_loading_current)
        AnimeListImportPhase.APPLYING_CHANGES ->
            stringResource(R.string.library_transfer_phase_applying, processed, total)
    }

fun AnimeListImportProgress.fractionOrNull(): Float? =
    when (phase) {
        AnimeListImportPhase.LOADING_CURRENT_LIST -> null
        AnimeListImportPhase.RESOLVING_TITLES,
        AnimeListImportPhase.APPLYING_CHANGES,
        -> if (total > 0) (processed.toFloat() / total.toFloat()).coerceIn(0f, 1f) else null
    }
