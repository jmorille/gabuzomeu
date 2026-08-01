package eu.ttbox.gabuzomeu.ui.help

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import eu.ttbox.gabuzomeu.ui.HelpTags
import eu.ttbox.gabuzomeu.ui.theme.GabuzomeuTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * L'écran d'apprentissage.
 *
 * Par `testTag` uniquement : tout le contenu de cet écran est du texte traduit, et la CI tourne
 * en `en-US`. Ce qui se vérifie ici, c'est la structure — l'écran s'affiche, il se referme —
 * pas la prose.
 *
 * La table des entiers, elle, n'a pas besoin de test d'interface : elle est **calculée** par
 * `ShadokConverter`, déjà couvert sur la JVM. C'est tout l'intérêt de ne rien y écrire à la main.
 */
class ShadokHelpScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(onClose: () -> Unit = {}) {
        composeTestRule.setContent {
            GabuzomeuTheme(dynamicColor = false) {
                ShadokHelpScreen(onClose = onClose)
            }
        }
    }

    @Test
    fun leLienVersLaVideoEstVisibleSansDefiler() {
        setScreen()

        composeTestRule.onNodeWithTag(HelpTags.SCREEN).assertIsDisplayed()
        // **Sans `performScrollTo`**, et c'est tout l'objet de ce test : la vidéo est la leçon
        // d'origine, cette page n'en est que la transcription. Reléguée en pied d'écran, il
        // fallait défiler pour la découvrir. Elle doit être là à l'ouverture.
        composeTestRule.onNodeWithTag(HelpTags.VIDEO).assertIsDisplayed()
    }

    @Test
    fun laLeconEntiereEstAtteignableEnDefilant() {
        setScreen()

        // La fin de la leçon est plusieurs écrans plus bas : ce qui se vérifie ici, c'est que
        // la colonne défile bien jusqu'au bout, et non que le texte est celui-ci ou celui-là.
        composeTestRule.onNodeWithTag(HelpTags.ENDING).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun laFlecheReferme() {
        var closed = false
        setScreen(onClose = { closed = true })

        composeTestRule.onNodeWithTag(HelpTags.CLOSE).performClick()

        assertEquals(true, closed)
    }
}
