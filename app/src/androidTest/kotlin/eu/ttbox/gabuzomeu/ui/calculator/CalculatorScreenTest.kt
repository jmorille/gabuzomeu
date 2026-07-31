package eu.ttbox.gabuzomeu.ui.calculator

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.ui.theme.GabuzomeuTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests instrumentés de l'écran.
 *
 * Le test d'origine, `CalculatorHitSomeButtons`, ne pouvait plus passer du tout : il
 * castait `display.getCurrentView()` en `EditText` alors que les enfants du
 * `ViewSwitcher` étaient devenus des `CalculatorConverterDisplay` (`LinearLayout`) —
 * `ClassCastException` garantie.
 */
class CalculatorScreenTest {

    /**
     * `createAndroidComposeRule` de l'API **v2** : elle s'appuie sur
     * `StandardTestDispatcher`, aligné sur le comportement standard des coroutines, là
     * où la v1 — dépréciée — exécutait les tâches immédiatement via
     * `UnconfinedTestDispatcher`.
     */
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private fun setScreen(
        state: CalculatorUiState,
        widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
        onKey: (KeyAction) -> Unit = {},
        onNotationChange: (NumberNotation) -> Unit = {},
    ) {
        composeTestRule.setContent {
            // dynamicColor = false : un rendu stable, indépendant du fond d'écran.
            GabuzomeuTheme(dynamicColor = false) {
                CalculatorScreen(
                    state = state,
                    widthSizeClass = widthSizeClass,
                    onKey = onKey,
                    onNotationChange = onNotationChange,
                )
            }
        }
    }

    @Test
    fun lesTroisEcrituresSontAffichees() {
        setScreen(
            CalculatorUiState(decimal = "6", glyphs = "_⅃", labels = "BuZo"),
        )

        composeTestRule.onNodeWithText("6").assertIsDisplayed()
        composeTestRule.onNodeWithText("BuZo").assertIsDisplayed()
        // La ligne de glyphes est vectorielle : elle s'annonce par ses noms.
        composeTestRule.onNodeWithContentDescription("BuZo").assertIsDisplayed()
    }

    @Test
    fun lePaveDecimalEmetLesBonnesActions() {
        val pressed = mutableListOf<KeyAction>()
        setScreen(CalculatorUiState(), onKey = { pressed += it })

        composeTestRule.onNodeWithText("7").performClick()
        composeTestRule.onNodeWithContentDescription("Multiplié par").performClick()
        composeTestRule.onNodeWithText("6").performClick()
        composeTestRule.onNodeWithContentDescription("Égale").performClick()

        assertEquals(4, pressed.size)
        assertEquals(KeyAction.Digit('7'), pressed[0])
        assertEquals(KeyAction.Digit('6'), pressed[2])
        assertEquals(KeyAction.Evaluate, pressed[3])
    }

    /**
     * Les touches Shadok s'annoncent par leur **nom**, pas par leur forme.
     *
     * C'est la garantie structurelle contre le bug d'origine : dans
     * `res/layout-port/shadok_pad.xml:32-42`, `@+id/digitMeu` portait
     * `contentDescription="@string/digitNameMeu"` mais `text="@string/digitZo"` — les
     * descriptions de Zo et Meu étaient croisées.
     */
    @Test
    fun lesTouchesShadokSAnnoncentParLeurNom() {
        val pressed = mutableListOf<KeyAction>()
        setScreen(
            CalculatorUiState(notation = NumberNotation.SHADOK),
            onKey = { pressed += it },
        )

        listOf("Ga" to '◯', "Bu" to '_', "Zo" to '⅃', "Meu" to '◿').forEach { (label, glyph) ->
            pressed.clear()
            composeTestRule.onNodeWithContentDescription(label).performClick()
            assertEquals(
                KeyAction.Digit(glyph),
                pressed.single(),
                "la touche annoncee « $label » doit saisir le glyphe $glyph",
            )
        }
    }

    @Test
    fun lePaveShadokNAffichePasDeChiffresDecimaux() {
        setScreen(CalculatorUiState(notation = NumberNotation.SHADOK))

        // En base 4, les chiffres 4 a 9 n'existent pas.
        listOf("4", "5", "6", "7", "8", "9").forEach { absent ->
            assertTrue(
                composeTestRule.onAllNodesWithText(absent).fetchSemanticsNodes().isEmpty(),
                "le chiffre $absent ne doit pas figurer sur le pave Shadok",
            )
        }
    }

    @Test
    fun leSelecteurDeModeChangeDeNotation() {
        var requested: NumberNotation? = null
        setScreen(CalculatorUiState(), onNotationChange = { requested = it })

        composeTestRule.onNodeWithText("Shadok").performClick()

        assertEquals(NumberNotation.SHADOK, requested)
    }

    /**
     * Sur grand écran, le pavé Shadok doit être là. Les dispositions tablette du projet
     * d'origine (`res/layout-sw600dp/main.xml` et `-sw600dp-land`) n'avaient aucune
     * touche Shadok ni `@+id/panelswitch` : la fonctionnalité était inaccessible.
     */
    @Test
    fun lePaveShadokResteAccessibleSurGrandEcran() {
        setScreen(
            CalculatorUiState(notation = NumberNotation.SHADOK),
            widthSizeClass = WindowWidthSizeClass.Expanded,
        )

        listOf("Ga", "Bu", "Zo", "Meu").forEach { label ->
            composeTestRule.onNodeWithContentDescription(label).assertIsDisplayed()
        }
    }
}
