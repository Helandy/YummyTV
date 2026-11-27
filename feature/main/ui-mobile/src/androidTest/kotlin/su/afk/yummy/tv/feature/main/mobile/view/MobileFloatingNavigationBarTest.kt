package su.afk.yummy.tv.feature.main.mobile.view

import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme
import su.afk.yummy.tv.feature.main.mobile.model.MobileMenuItem

class MobileFloatingNavigationBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun selectionAndDescriptionsSurviveTheExpandingLabel() {
        val labels = listOf("Community", "Catalog", "Home", "Library", "Profile")
        val items = labels.mapIndexed { index, label -> MobileMenuItem(label, index, Icons.Default.Home) }
        var selected by mutableStateOf(2)

        composeRule.setContent {
            ScreenPreviewTheme {
                androidx.compose.foundation.layout.Box(Modifier.width(320.dp)) {
                    MobileFloatingNavigationBar(
                        selectedDestination = selected,
                        menuItems = items,
                        upFocusRequester = null,
                        onDestinationSelected = { selected = it },
                    )
                }
            }
        }

        labels.forEach { composeRule.onNodeWithContentDescription(it).assertIsDisplayed() }
        composeRule.onNodeWithContentDescription("Home").assertIsSelected()
        composeRule.onNodeWithContentDescription("Library").performClick().assertIsSelected()
    }
}
