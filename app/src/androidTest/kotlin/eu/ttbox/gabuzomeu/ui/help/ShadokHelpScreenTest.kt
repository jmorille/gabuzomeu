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
    fun lEcranSAfficheAvecSonLienSortant() {
        setScreen()

        composeTestRule.onNodeWithTag(HelpTags.SCREEN).assertIsDisplayed()
        // Le lien est en pied de page : sur un écran de téléphone il est hors champ au
        // départ, et « affiché » n'a de sens qu'après y avoir défilé.
        composeTestRule.onNodeWithTag(HelpTags.VIDEO).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun laFlecheReferme() {
        var closed = false
        setScreen(onClose = { closed = true })

        composeTestRule.onNodeWithTag(HelpTags.CLOSE).performClick()

        assertEquals(true, closed)
    }
}
