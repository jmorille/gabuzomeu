package eu.ttbox.gabuzomeu.ui.game

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import eu.ttbox.gabuzomeu.ui.GameTags
import eu.ttbox.gabuzomeu.ui.theme.GabuzomeuTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Le jeu, côté interface.
 *
 * Tout ce qui relève des questions elles-mêmes — leurres, difficulté, invariants — est vérifié
 * sur la JVM dans `ShadokQuizTest`, où le tirage est reproductible. Ici on ne vérifie que ce qui
 * exige un rendu réel : les quatre boutons existent, et répondre les verrouille.
 */
class ShadokQuizScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(onClose: () -> Unit = {}) {
        composeTestRule.setContent {
            GabuzomeuTheme(dynamicColor = false) {
                ShadokQuizScreen(onClose = onClose)
            }
        }
    }

    @Test
    fun uneQuestionEtQuatreReponsesSAffichent() {
        setScreen()

        composeTestRule.onNodeWithTag(GameTags.SCREEN).assertIsDisplayed()
        composeTestRule.onNodeWithTag(GameTags.QUESTION).assertIsDisplayed()
        composeTestRule.onNodeWithTag(GameTags.SCORE).assertIsDisplayed()
        // Présence, pas visibilité : l'écran défile, et la dernière réponse peut être sous le
        // pli sur un petit émulateur. Ce qui est vérifié ici, c'est qu'il y en a bien quatre.
        repeat(ShadokQuiz.CHOICE_COUNT) { index ->
            composeTestRule.onNodeWithTag(GameTags.choice(index)).assertExists()
        }
    }

    @Test
    fun repondreVerrouilleLesChoix() {
        // Sans verrou, recliquer ferait monter le score sans changer la question.
        setScreen()

        composeTestRule.onNodeWithTag(GameTags.choice(0)).performClick()

        repeat(ShadokQuiz.CHOICE_COUNT) { index ->
            composeTestRule.onNodeWithTag(GameTags.choice(index)).assertIsNotEnabled()
        }
    }

    @Test
    fun laFlecheReferme() {
        var closed = false
        setScreen(onClose = { closed = true })

        composeTestRule.onNodeWithTag(GameTags.CLOSE).performClick()

        assertEquals(true, closed)
    }
}
