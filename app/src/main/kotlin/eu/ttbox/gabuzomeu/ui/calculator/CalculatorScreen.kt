package eu.ttbox.gabuzomeu.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.data.DisplaySettings
import eu.ttbox.gabuzomeu.ui.calculator.components.CalculatorMenu
import eu.ttbox.gabuzomeu.ui.calculator.components.InputModeSelector
import eu.ttbox.gabuzomeu.ui.calculator.components.Keypad
import eu.ttbox.gabuzomeu.ui.calculator.components.ShadokDisplay
import kotlinx.coroutines.launch

/**
 * L'écran de la calculatrice.
 *
 * Une seule définition d'interface, adaptée à la largeur disponible — là où le projet
 * d'origine avait quatre dispositions XML de `main.xml` (`layout-port`, `layout-land`,
 * `layout-sw600dp`, `layout-sw600dp-land`) dont **deux avaient oublié les touches
 * Shadok**, rendant la fonctionnalité inaccessible sur tablette.
 */
@Composable
fun CalculatorScreen(
    state: CalculatorUiState,
    widthSizeClass: WindowWidthSizeClass,
    onKey: (KeyAction) -> Unit,
    onNotationChange: (NumberNotation) -> Unit,
    onModeChange: (CalculationMode) -> Unit,
    onSettingsChange: (DisplaySettings) -> Unit,
    modifier: Modifier = Modifier,
    onPaste: (String) -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val copied = stringResource(R.string.copy_done)

    // Ce rappel n'est déclenché qu'en dessous d'Android 13, où le système n'affiche aucune
    // confirmation de copie de lui-même — la décision est prise dans DisplayActionsMenu, au
    // plus près de l'appel au presse-papiers.
    val onCopied: () -> Unit = {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(copied)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { insets ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            color = MaterialTheme.colorScheme.surface,
        ) {
            val actions = CalculatorActions(
                onKey = onKey,
                onNotationChange = onNotationChange,
                onModeChange = onModeChange,
                onSettingsChange = onSettingsChange,
                onPaste = onPaste,
                onCopied = onCopied,
            )
            if (widthSizeClass == WindowWidthSizeClass.Expanded) {
                WideLayout(state, actions)
            } else {
                StackedLayout(state, actions)
            }
        }
    }
}

/**
 * Les rappels de l'écran, regroupés.
 *
 * Six paramètres traversaient les deux dispositions : les passer un à un rendait chaque
 * signature illisible et faisait échouer la règle `LongParameterList` de Detekt. Ils forment
 * un tout — ce que l'écran sait faire — et voyagent donc ensemble.
 */
private data class CalculatorActions(
    val onKey: (KeyAction) -> Unit,
    val onNotationChange: (NumberNotation) -> Unit,
    val onModeChange: (CalculationMode) -> Unit,
    val onSettingsChange: (DisplaySettings) -> Unit,
    val onPaste: (String) -> Unit,
    val onCopied: () -> Unit,
)

/** Téléphone en portrait, ou fenêtre étroite : afficheur au-dessus, pavé en dessous. */
@Composable
private fun StackedLayout(state: CalculatorUiState, actions: CalculatorActions) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            CalculatorMenu(
                mode = state.mode,
                settings = state.settings,
                onModeChange = actions.onModeChange,
                onSettingsChange = actions.onSettingsChange,
            )
        }
        ShadokDisplay(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(DISPLAY_WEIGHT),
            onPaste = actions.onPaste,
            onCopied = actions.onCopied,
        )
        InputModeSelector(
            notation = state.notation,
            onNotationChange = actions.onNotationChange,
        )
        Keypad(
            mode = state.mode,
            notation = state.notation,
            onKey = actions.onKey,
            modifier = Modifier
                .fillMaxWidth()
                .weight(KEYPAD_WEIGHT)
                .padding(all = 16.dp),
        )
    }
}

/** Tablette ou paysage large : afficheur et pavé côte à côte. */
@Composable
private fun WideLayout(state: CalculatorUiState, actions: CalculatorActions) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            // En NPI, la pile occupe la hauteur disponible : la centrer la comprimerait.
            verticalArrangement = if (state.mode == CalculationMode.RPN) {
                Arrangement.Bottom
            } else {
                Arrangement.Center
            },
            horizontalAlignment = Alignment.End,
        ) {
            CalculatorMenu(
                mode = state.mode,
                settings = state.settings,
                onModeChange = actions.onModeChange,
                onSettingsChange = actions.onSettingsChange,
            )
            ShadokDisplay(
                state = state,
                modifier = if (state.mode == CalculationMode.RPN) Modifier.weight(1f) else Modifier,
                onPaste = actions.onPaste,
                onCopied = actions.onCopied,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InputModeSelector(
                notation = state.notation,
                onNotationChange = actions.onNotationChange,
            )
            Keypad(
                mode = state.mode,
                notation = state.notation,
                onKey = actions.onKey,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * La part de hauteur réservée à l'afficheur — **la même dans les deux modes**.
 *
 * Elle a d'abord été plus généreuse en NPI, pour laisser voir quelques niveaux de pile. Mais
 * le pavé héritait du reste, si bien qu'il changeait de taille en basculant de mode : les
 * touches sautaient sous le doigt, et la mémoire gestuelle ne servait plus à rien. C'est le
 * pavé qui doit rester stable ; la pile, elle, se contente de la place restante et défile.
 */
private const val DISPLAY_WEIGHT = 1.5f
private const val KEYPAD_WEIGHT = 2f
