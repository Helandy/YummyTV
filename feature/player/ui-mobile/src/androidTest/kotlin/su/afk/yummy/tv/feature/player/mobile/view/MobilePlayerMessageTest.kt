package su.afk.yummy.tv.feature.player.mobile.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import su.afk.yummy.tv.core.designsystem.preview.ScreenPreviewTheme

/**
 * Сообщение об ошибке плеера. Сам PlayerMobileScreen в instrumented-тесте не поднять: он стартует
 * Hilt-сервис воспроизведения, а тестового Hilt-приложения в модуле нет.
 */
class MobilePlayerMessageTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()

    private fun setMessage(
        title: String? = "Не удалось получить поток",
        actionLabel: String? = null,
        secondaryLabel: String? = null,
        tertiaryLabel: String? = null,
    ) {
        composeRule.setContent {
            ScreenPreviewTheme {
                MobilePlayerMessage(
                    title = title,
                    onBack = { calls += "back" },
                    actionLabel = actionLabel,
                    onAction = { calls += "action" },
                    secondaryActionLabel = secondaryLabel,
                    onSecondaryAction = { calls += "secondary" },
                    tertiaryActionLabel = tertiaryLabel,
                    onTertiaryAction = { calls += "tertiary" },
                )
            }
        }
    }

    @Test
    fun titleIsShown() {
        setMessage()

        composeRule.onNodeWithText("Не удалось получить поток").assertIsDisplayed()
    }

    @Test
    fun withoutActionLabelNoButtonIsShown() {
        setMessage(actionLabel = null)

        composeRule.onNodeWithText("Повторить").assertDoesNotExist()
    }

    @Test
    fun actionButtonCallsOnAction() {
        setMessage(actionLabel = "Повторить")

        composeRule.onNodeWithText("Повторить").performClick()

        assertEquals(listOf("action"), calls)
    }

    @Test
    fun secondaryAndTertiaryButtonsCallTheirCallbacks() {
        setMessage(secondaryLabel = "Сменить плеер", tertiaryLabel = "Сменить озвучку")

        composeRule.onNodeWithText("Сменить плеер").performClick()
        composeRule.onNodeWithText("Сменить озвучку").performClick()

        assertEquals(listOf("secondary", "tertiary"), calls)
    }

    @Test
    fun backIconCallsOnBack() {
        setMessage()

        // кнопка «назад» — единственная кликабельная область без текста
        composeRule.onNode(hasClickAction() and !hasText("Не удалось получить поток")).performClick()

        assertEquals(listOf("back"), calls)
    }
}
