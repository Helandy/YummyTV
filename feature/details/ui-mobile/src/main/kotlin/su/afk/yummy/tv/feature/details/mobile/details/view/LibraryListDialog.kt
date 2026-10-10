package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import su.afk.yummy.tv.domain.account.model.UserAnimeList
import su.afk.yummy.tv.feature.details.mobile.details.model.MobilePickerItem
import su.afk.yummy.tv.feature.details.mobile.details.utils.label
import su.afk.yummy.tv.feature.details.presentation.R
import su.afk.yummy.tv.feature.details.utils.statusColor

@Composable
internal fun LibraryListDialog(
    onSelected: (UserAnimeList) -> Unit,
    onDismiss: () -> Unit,
) {
    val options = listOf(
        UserAnimeList.WATCHING,
        UserAnimeList.PLANNED,
        UserAnimeList.COMPLETED,
        UserAnimeList.POSTPONED,
        UserAnimeList.DROPPED,
    )
    MobilePickerBottomSheet(
        title = stringResource(R.string.details_library_picker_title),
        onDismiss = onDismiss,
    ) {
        MobilePickerItems(
            items = options.map { option ->
                MobilePickerItem(
                    key = option.name,
                    title = option.label(),
                    color = option.statusColor(),
                    onClick = { onSelected(option) },
                )
            },
        )
    }
}
