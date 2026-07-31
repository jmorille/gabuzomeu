package eu.ttbox.gabuzomeu.ui.calculator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.data.DisplaySettings
import eu.ttbox.gabuzomeu.ui.calculator.components.DisplaySettingsMenu
import eu.ttbox.gabuzomeu.ui.calculator.components.InputModeSelector
import eu.ttbox.gabuzomeu.ui.calculator.components.Keypad
import eu.ttbox.gabuzomeu.ui.calculator.components.ShadokDisplay

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
    onSettingsChange: (DisplaySettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(modifier = modifier.fillMaxSize()) { insets ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets),
            color = MaterialTheme.colorScheme.surface,
        ) {
            if (widthSizeClass == WindowWidthSizeClass.Expanded) {
                WideLayout(state, onKey, onNotationChange, onSettingsChange)
            } else {
                StackedLayout(state, onKey, onNotationChange, onSettingsChange)
            }
        }
    }
}

/** Téléphone en portrait, ou fenêtre étroite : afficheur au-dessus, pavé en dessous. */
@Composable
private fun StackedLayout(
    state: CalculatorUiState,
    onKey: (KeyAction) -> Unit,
    onNotationChange: (NumberNotation) -> Unit,
    onSettingsChange: (DisplaySettings) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            DisplaySettingsMenu(settings = state.settings, onSettingsChange = onSettingsChange)
        }
        ShadokDisplay(
            state = state,
            modifier = Modifier
                .fillMaxWidth()
                .weight(DISPLAY_WEIGHT),
        )
        InputModeSelector(notation = state.notation, onNotationChange = onNotationChange)
        Keypad(
            notation = state.notation,
            onKey = onKey,
            modifier = Modifier
                .fillMaxWidth()
                .weight(KEYPAD_WEIGHT)
                .padding(all = 16.dp),
        )
    }
}

/** Tablette ou paysage large : afficheur et pavé côte à côte. */
@Composable
private fun WideLayout(
    state: CalculatorUiState,
    onKey: (KeyAction) -> Unit,
    onNotationChange: (NumberNotation) -> Unit,
    onSettingsChange: (DisplaySettings) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.End,
        ) {
            DisplaySettingsMenu(settings = state.settings, onSettingsChange = onSettingsChange)
            ShadokDisplay(state = state)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
                .padding(all = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InputModeSelector(notation = state.notation, onNotationChange = onNotationChange)
            Keypad(notation = state.notation, onKey = onKey, modifier = Modifier.weight(1f))
        }
    }
}

private const val DISPLAY_WEIGHT = 1f
private const val KEYPAD_WEIGHT = 2f
