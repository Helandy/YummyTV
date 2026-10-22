package su.afk.yummy.tv.feature.details.mobile.details.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import su.afk.yummy.tv.feature.details.mobile.details.model.DetailsLink

/** Плоский список строк-ссылок (рецензии, комментарии) без карточек и обводок. */
@Composable
internal fun DetailsLinksCard(
    links: List<DetailsLink>,
    modifier: Modifier = Modifier,
) {
    if (links.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("details_links_card"),
    ) {
        links.forEach { link -> DetailsLinkRow(link) }
    }
}
