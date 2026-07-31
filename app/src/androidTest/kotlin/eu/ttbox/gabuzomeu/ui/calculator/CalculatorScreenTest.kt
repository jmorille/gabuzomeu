package eu.ttbox.gabuzomeu.ui.calculator

import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.ui.test.assertContentDescriptionContains
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.core.eval.Operator
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import eu.ttbox.gabuzomeu.data.DisplaySettings
import eu.ttbox.gabuzomeu.ui.DisplayTags
import eu.ttbox.gabuzomeu.ui.KeypadTags
import eu.ttbox.gabuzomeu.ui.SettingsTags
import eu.ttbox.gabuzomeu.ui.theme.GabuzomeuTheme
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Tests instrumentés de l'écran.
 *
 * Deux principes, appris à la dure :
 *
 * 1. **Rien en dur qui dépende de la langue.** Une première version comparait des
 *    chaînes françaises ; elles passaient sur un téléphone français et échouaient sur
 *    l'émulateur `en-US` de la CI, qui sert `values-en/`.
 * 2. **Pas de recherche par texte sur les touches.** Le pavé applique
 *    `clearAndSetSemantics`, ce qui efface la sémantique de texte des descendants : une
 *    touche est introuvable par `onNodeWithText`. On passe donc par des `testTag`
 *    dérivés de l'action.
 *
 * Le test d'origine, `CalculatorHitSomeButtons`, ne pouvait de toute façon plus passer :
 * il castait `display.getCurrentView()` en `EditText` alors que les enfants du
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

    /**
     * Les rappels de l'écran, regroupés.
     *
     * Un test n'en observe jamais qu'un ; les autres gardent leur lambda vide. Les passer
     * en bloc évite aussi de faire grandir la liste de paramètres de [setScreen] à chaque
     * nouvelle interaction — detekt s'en plaint, à juste titre.
     */
    private class Callbacks(
        val onKey: (KeyAction) -> Unit = {},
        val onNotationChange: (NumberNotation) -> Unit = {},
        val onModeChange: (CalculationMode) -> Unit = {},
        val onSettingsChange: (DisplaySettings) -> Unit = {},
    )

    private fun setScreen(
        state: CalculatorUiState,
        widthSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
        callbacks: Callbacks = Callbacks(),
    ) {
        composeTestRule.setContent {
            // dynamicColor = false : un rendu stable, indépendant du fond d'écran.
            GabuzomeuTheme(dynamicColor = false) {
                CalculatorScreen(
                    state = state,
                    widthSizeClass = widthSizeClass,
                    onKey = callbacks.onKey,
                    onNotationChange = callbacks.onNotationChange,
                    onModeChange = callbacks.onModeChange,
                    onSettingsChange = callbacks.onSettingsChange,
                )
            }
        }
    }

    // ------------------------------------------------------------------ affichage

    @Test
    fun leShadokEstLaLignePrincipaleEtLeDecimalEstSecondaire() {
        setScreen(CalculatorUiState(glyphs = "_⅃", labels = "BuZo", decimal = "6"))

        // La ligne de glyphes est vectorielle : elle s'annonce par ses noms.
        composeTestRule.onNodeWithTag(DisplayTags.GLYPHS)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("BuZo")
        composeTestRule.onNodeWithTag(DisplayTags.LABELS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DisplayTags.DECIMAL).assertIsDisplayed()
    }

    @Test
    fun masquerLesNomsShadokRetireLaLigneCorrespondante() {
        setScreen(
            CalculatorUiState(
                glyphs = "_⅃",
                labels = "BuZo",
                decimal = "6",
                settings = DisplaySettings(showShadokLabels = false),
            ),
        )

        composeTestRule.onNodeWithTag(DisplayTags.LABELS).assertDoesNotExist()
        // Les glyphes restent : ils ne sont pas masquables.
        composeTestRule.onNodeWithTag(DisplayTags.GLYPHS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DisplayTags.DECIMAL).assertIsDisplayed()
    }

    @Test
    fun masquerLeDecimalRetireLaLigneCorrespondante() {
        setScreen(
            CalculatorUiState(
                glyphs = "_⅃",
                labels = "BuZo",
                decimal = "6",
                settings = DisplaySettings(showDecimal = false),
            ),
        )

        composeTestRule.onNodeWithTag(DisplayTags.DECIMAL).assertDoesNotExist()
        composeTestRule.onNodeWithTag(DisplayTags.GLYPHS).assertIsDisplayed()
        composeTestRule.onNodeWithTag(DisplayTags.LABELS).assertIsDisplayed()
    }

    @Test
    fun lesGlyphesRestentVisiblesMemeToutMasque() {
        setScreen(
            CalculatorUiState(
                glyphs = "_⅃",
                labels = "BuZo",
                decimal = "6",
                settings = DisplaySettings(showShadokLabels = false, showDecimal = false),
            ),
        )

        composeTestRule.onNodeWithTag(DisplayTags.GLYPHS).assertIsDisplayed()
    }

    // -------------------------------------------------------------------- réglages

    @Test
    fun leMenuPermetDeBasculerLesReglages() {
        var requested: DisplaySettings? = null
        setScreen(CalculatorUiState(), callbacks = Callbacks(onSettingsChange = { requested = it }))

        composeTestRule.onNodeWithTag(SettingsTags.MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(SettingsTags.TOGGLE_LABELS).performClick()

        assertEquals(DisplaySettings(showShadokLabels = false), requested)
    }

    @Test
    fun leMenuPermetDeMasquerLeDecimal() {
        var requested: DisplaySettings? = null
        setScreen(CalculatorUiState(), callbacks = Callbacks(onSettingsChange = { requested = it }))

        composeTestRule.onNodeWithTag(SettingsTags.MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(SettingsTags.TOGGLE_DECIMAL).performClick()

        assertEquals(DisplaySettings(showDecimal = false), requested)
    }

    // ------------------------------------------------------------------------ pavé

    @Test
    fun lePaveDecimalEmetLesBonnesActions() {
        val pressed = mutableListOf<KeyAction>()
        setScreen(CalculatorUiState(), callbacks = Callbacks(onKey = { pressed += it }))

        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Digit('7'))).performClick()
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Op(Operator.TIMES))).performClick()
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Digit('6'))).performClick()
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Evaluate)).performClick()

        assertEquals(
            listOf(
                KeyAction.Digit('7'),
                KeyAction.Op(Operator.TIMES),
                KeyAction.Digit('6'),
                KeyAction.Evaluate,
            ),
            pressed,
        )
    }

    /**
     * Les touches Shadok s'annoncent par leur **nom**, pas par leur forme.
     *
     * C'est la garantie structurelle contre le bug d'origine : dans
     * `res/layout-port/shadok_pad.xml:32-42`, `@+id/digitMeu` portait
     * `contentDescription="@string/digitNameMeu"` mais `text="@string/digitZo"` — les
     * descriptions de Zo et Meu étaient croisées. Elles dérivent désormais de
     * [ShadokDigit.label], donc l'inversion est impossible.
     */
    @Test
    fun lesTouchesShadokSAnnoncentParLeurNom() {
        val pressed = mutableListOf<KeyAction>()
        setScreen(
            CalculatorUiState(notation = NumberNotation.SHADOK),
            callbacks = Callbacks(onKey = {
                pressed +=
                    it
            }),
        )

        ShadokDigit.entries.forEach { digit ->
            pressed.clear()
            // Trouvée par son nom prononcé, cliquée, et vérifiée sur le glyphe émis.
            composeTestRule.onNodeWithContentDescription(digit.label).performClick()
            assertEquals(
                KeyAction.Digit(digit.glyph),
                pressed.single(),
                "la touche annoncée « ${digit.label} » doit saisir le glyphe ${digit.glyph}",
            )
        }
    }

    @Test
    fun lePaveShadokNAffichePasDeChiffresDecimaux() {
        setScreen(CalculatorUiState(notation = NumberNotation.SHADOK))

        // En base 4, les chiffres 4 à 9 n'existent pas.
        "456789".forEach { absent ->
            composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Digit(absent)))
                .assertDoesNotExist()
        }
        // Les quatre chiffres Shadok, eux, sont bien là.
        ShadokDigit.entries.forEach { digit ->
            composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Digit(digit.glyph)))
                .assertIsDisplayed()
        }
    }

    @Test
    fun leSelecteurDeModeChangeDeNotation() {
        var requested: NumberNotation? = null
        setScreen(CalculatorUiState(), callbacks = Callbacks(onNotationChange = { requested = it }))

        // Le sélecteur affiche des libellés traduits : on résout la ressource.
        val shadokLabel = composeTestRule.activity.getString(
            eu.ttbox.gabuzomeu.R.string.mode_shadok,
        )
        composeTestRule.onNodeWithText(shadokLabel).performClick()

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

        ShadokDigit.entries.forEach { digit ->
            composeTestRule.onNodeWithContentDescription(digit.label).assertIsDisplayed()
        }
    }

    // ------------------------------------------------------ notation polonaise inverse

    private val rpn = CalculatorUiState(mode = CalculationMode.RPN)

    @Test
    fun leMenuPermetDeChoisirLaNotationPolonaiseInverse() {
        var requested: CalculationMode? = null
        setScreen(CalculatorUiState(), callbacks = Callbacks(onModeChange = { requested = it }))

        composeTestRule.onNodeWithTag(SettingsTags.MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(SettingsTags.MODE_RPN).performClick()

        assertEquals(CalculationMode.RPN, requested)
    }

    @Test
    fun leMenuPermetDeRevenirALaCalculatriceClassique() {
        var requested: CalculationMode? = null
        setScreen(rpn, callbacks = Callbacks(onModeChange = { requested = it }))

        composeTestRule.onNodeWithTag(SettingsTags.MENU_BUTTON).performClick()
        composeTestRule.onNodeWithTag(SettingsTags.MODE_CLASSIC).performClick()

        assertEquals(CalculationMode.CLASSIC, requested)
    }

    /**
     * En postfixe, l'ordre de frappe est l'ordre de calcul : il n'y a rien à grouper ni à
     * déclencher. Les touches correspondantes doivent donc disparaître, et non rester
     * présentes sans effet.
     */
    @Test
    fun lePaveNpiNAffichePasDeParenthesesNiDEgale() {
        setScreen(rpn)

        listOf(KeyAction.LeftParen, KeyAction.RightParen, KeyAction.Evaluate).forEach { absent ->
            composeTestRule.onNodeWithTag(KeypadTags.of(absent)).assertDoesNotExist()
        }
        listOf(KeyAction.Enter, KeyAction.Swap, KeyAction.Drop, KeyAction.Negate).forEach { key ->
            composeTestRule.onNodeWithTag(KeypadTags.of(key)).assertIsDisplayed()
        }
    }

    @Test
    fun lePaveClassiqueNAffichePasLesTouchesDePile() {
        setScreen(CalculatorUiState())

        listOf(
            KeyAction.Enter,
            KeyAction.Swap,
            KeyAction.Drop,
            KeyAction.Negate,
        ).forEach { absent ->
            composeTestRule.onNodeWithTag(KeypadTags.of(absent)).assertDoesNotExist()
        }
    }

    @Test
    fun lePaveNpiEmetLesBonnesActions() {
        val pressed = mutableListOf<KeyAction>()
        setScreen(rpn, callbacks = Callbacks(onKey = { pressed += it }))

        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Digit('6'))).performClick()
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Enter)).performClick()
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Digit('7'))).performClick()
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Op(Operator.TIMES))).performClick()

        assertEquals(
            listOf(
                KeyAction.Digit('6'),
                KeyAction.Enter,
                KeyAction.Digit('7'),
                KeyAction.Op(Operator.TIMES),
            ),
            pressed,
        )
    }

    @Test
    fun lePaveNpiShadokGardeSesQuatreChiffresEtSesTouchesDePile() {
        setScreen(
            CalculatorUiState(mode = CalculationMode.RPN, notation = NumberNotation.SHADOK),
            widthSizeClass = WindowWidthSizeClass.Expanded,
        )

        ShadokDigit.entries.forEach { digit ->
            composeTestRule.onNodeWithContentDescription(digit.label).assertIsDisplayed()
        }
        composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Enter)).assertIsDisplayed()
        // En base 4, les chiffres 4 a 9 n'existent pas, NPI ou non.
        "456789".forEach { absent ->
            composeTestRule.onNodeWithTag(KeypadTags.of(KeyAction.Digit(absent)))
                .assertDoesNotExist()
        }
    }

    // -------------------------------------------------------------------- la pile

    @Test
    fun lesNiveauxDePileSontAffichesDuSommetVersLeFond() {
        setScreen(
            rpn.copy(
                glyphs = "_⅃",
                labels = "BuZo",
                decimal = "6",
                stack = listOf(
                    StackLevel(glyphs = "⅃", labels = "Zo", decimal = "2"),
                    StackLevel(glyphs = "_◿", labels = "BuMeu", decimal = "7"),
                ),
            ),
        )

        composeTestRule.onNodeWithTag(DisplayTags.STACK).assertIsDisplayed()
        // L'indice 0 est le niveau immediatement sous X, donc le dernier de la liste.
        composeTestRule.onNodeWithTag(DisplayTags.stackLevel(0))
            .assertIsDisplayed()
            .assertContentDescriptionContains("BuMeu", substring = true)
        composeTestRule.onNodeWithTag(DisplayTags.stackLevel(1))
            .assertIsDisplayed()
            .assertContentDescriptionContains("Zo", substring = true)
        // Et X reste la ligne principale.
        composeTestRule.onNodeWithTag(DisplayTags.GLYPHS).assertContentDescriptionEquals("BuZo")
    }

    /**
     * Le réglage vaut pour toute la colonne : le masquer sur X et le laisser sur les
     * niveaux au-dessus donnerait un afficheur mi-figue mi-raisin.
     */
    @Test
    fun masquerLeDecimalLeRetireAussiDeLaPile() {
        setScreen(
            rpn.copy(
                glyphs = "_⅃",
                labels = "BuZo",
                decimal = "6",
                stack = listOf(StackLevel(glyphs = "⅃", labels = "Zo", decimal = "2")),
                settings = DisplaySettings(showDecimal = false),
            ),
        )

        composeTestRule.onNodeWithTag(DisplayTags.DECIMAL).assertDoesNotExist()
        // Le niveau reste affiché, mais ne s'annonce plus que par ses noms Shadok.
        composeTestRule.onNodeWithTag(DisplayTags.stackLevel(0))
            .assertIsDisplayed()
            .assertContentDescriptionContains("Zo", substring = true)
    }

    @Test
    fun laPileNExistePasEnModeClassique() {
        setScreen(CalculatorUiState(glyphs = "_⅃", labels = "BuZo", decimal = "6"))

        composeTestRule.onNodeWithTag(DisplayTags.STACK).assertDoesNotExist()
    }
}
