package eu.ttbox.gabuzomeu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import eu.ttbox.gabuzomeu.ui.calculator.CalculatorScreen
import eu.ttbox.gabuzomeu.ui.calculator.CalculatorViewModel
import eu.ttbox.gabuzomeu.ui.theme.GabuzomeuTheme

/**
 * L'unique activité de l'application.
 *
 * Le manifeste d'origine en déclarait deux avec le filtre `LAUNCHER` — `Calculator` et
 * `WidgetActivity`, cette dernière n'affichant qu'un `RelativeLayout` vide — donc **deux
 * icônes** installées, dont une menant à un écran blanc. Une troisième activité,
 * `MainActivity`, existait en code mais était entièrement commentée dans le manifeste.
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        // Bord à bord : l'application dessine sous les barres système, comme l'exige
        // Android 15+. Scaffold réinjecte les encarts.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            GabuzomeuTheme {
                val viewModel: CalculatorViewModel = viewModel(
                    factory = CalculatorViewModel.factory(this),
                )
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                CalculatorScreen(
                    state = state,
                    widthSizeClass = calculateWindowSizeClass(this).widthSizeClass,
                    onKey = viewModel::onKey,
                    onNotationChange = viewModel::onNotationChange,
                    onModeChange = viewModel::onModeChange,
                    onSettingsChange = viewModel::onSettingsChange,
                    onPaste = viewModel::onPaste,
                )
            }
        }
    }
}
