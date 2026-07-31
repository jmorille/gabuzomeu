package eu.ttbox.gabuzomeu

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.ttbox.gabuzomeu.core.eval.Operator
import eu.ttbox.gabuzomeu.ui.DisplayTags
import eu.ttbox.gabuzomeu.ui.KeypadTags
import eu.ttbox.gabuzomeu.ui.SettingsTags
import eu.ttbox.gabuzomeu.ui.calculator.KeyAction
import org.junit.Rule
import org.junit.Test

/**
 * L'application **réelle** : la vraie activité, le vrai ViewModel, le vrai DataStore.
 *
 * [eu.ttbox.gabuzomeu.ui.calculator.CalculatorScreenTest] pilote l'écran avec un état
 * figé ; il vérifie donc l'interface mais ne traverse jamais l'arithmétique. Ce trou a
 * laissé passer un plantage au premier chiffre tapé sur Android 12 : `Rational` employait
 * `BigInteger.TWO`, absent avant l'API 33, et son initialisation échouait d'un
 * `NoSuchFieldError`. Ni les tests JVM (où la constante existe), ni Android Lint (qui
 * n'inspecte pas `:core:shadok`, module Kotlin pur), ni l'appareil de développement (API
 * 37) ne pouvaient le voir.
 *
 * D'où ces quelques cas de bout en bout, volontairement peu nombreux : ils ne valident pas
 * le détail du comportement — les tests JVM s'en chargent — mais que le circuit complet
 * fonctionne **sur la version d'Android visée**.
 */
class MainActivityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    /**
     * Remet l'application dans un état connu.
     *
     * Nécessaire parce que l'activité restaure la dernière session depuis le disque : sans
     * cela, un test dépendrait de ce que le précédent a laissé. Le `waitForIdle` laisse
     * d'abord cette restauration se terminer, sans quoi elle écraserait la remise à zéro.
     */
    private fun startFresh(decimalKeypad: Boolean = true) {
        composeTestRule.waitForIdle()
        if (decimalKeypad) {
            val label = composeTestRule.activity.getString(R.string.mode_decimal)
            composeTestRule.onNodeWithText(label).performClick()
        }
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Clear)).performClick()
    }

    private fun selectMode(tag: String) {
        composeTestRule.onNodeWithTag(SettingsTags.MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(tag).performClick()
    }

    private fun press(vararg actions: KeyAction) = actions.forEach { action ->
        composeTestRule.onNodeWithTag(KeypadTags.of(action)).performClick()
    }

    @Test
    fun uneMultiplicationClassiqueTraverseToutLeCircuit() {
        startFresh()
        selectMode(SettingsTags.MODE_CLASSIC)
        startFresh()

        press(
            KeyAction.Digit('6'),
            KeyAction.Op(Operator.TIMES),
            KeyAction.Digit('7'),
            KeyAction.Evaluate,
        )

        // 42 en base 4 s'ecrit 222. La ligne de glyphes s'annonce par ses noms.
        composeTestRule.onNodeWithTag(DisplayTags.GLYPHS)
            .assertContentDescriptionEquals("ZoZoZo")
    }

    @Test
    fun uneMultiplicationEnNpiTraverseToutLeCircuit() {
        startFresh()
        selectMode(SettingsTags.MODE_RPN)
        startFresh()

        press(
            KeyAction.Digit('6'),
            KeyAction.Enter,
            KeyAction.Digit('7'),
            KeyAction.Op(Operator.TIMES),
        )

        composeTestRule.onNodeWithTag(DisplayTags.GLYPHS)
            .assertContentDescriptionEquals("ZoZoZo")
    }

    /**
     * Un tiers oblige `Rational` à décider si l'écriture décimale termine — le chemin même
     * qui plantait sur Android 12.
     */
    @Test
    fun unTiersSeCalculeSansPlanter() {
        startFresh()
        selectMode(SettingsTags.MODE_CLASSIC)
        startFresh()

        press(
            KeyAction.Digit('1'),
            KeyAction.Op(Operator.DIVIDE),
            KeyAction.Digit('3'),
            KeyAction.Evaluate,
        )

        // 1/3 = 0.111... en base 4 : un developpement infini, donc tronque.
        composeTestRule.onNodeWithTag(DisplayTags.DECIMAL).assertExists()
    }
}
