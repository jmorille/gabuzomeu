package eu.ttbox.gabuzomeu.ui.calculator.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.data.DisplaySettings
import eu.ttbox.gabuzomeu.ui.SettingsTags

/**
 * Le menu de la calculatrice : mode de calcul, puis réglages d'affichage.
 *
 * Le mode vient en tête parce qu'il change la nature de la machine, là où les deux
 * interrupteurs ne font qu'habiller l'afficheur. Il se choisit à la case ronde et non à
 * l'interrupteur : classique et NPI s'excluent, et un `Switch` étiqueté « NPI » laisserait
 * croire qu'on ajoute une option plutôt qu'on remplace le pavé.
 *
 * La ligne de glyphes n'est pas proposée au masquage : elle est le sujet de l'application,
 * la cacher viderait l'écran de son sens.
 */
@Composable
fun CalculatorMenu(
    mode: CalculationMode,
    settings: DisplaySettings,
    onModeChange: (CalculationMode) -> Unit,
    onSettingsChange: (DisplaySettings) -> Unit,
    modifier: Modifier = Modifier,
    onOpenHelp: () -> Unit = {},
    onOpenGame: () -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.testTag(SettingsTags.MENU_BUTTON),
        ) {
            Icon(
                imageVector = Icons.Filled.MoreVert,
                contentDescription = stringResource(R.string.settings_open),
            )
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            MenuHeading(stringResource(R.string.settings_mode))
            CalculationMode.entries.forEach { option ->
                ModeChoice(
                    label = stringResource(option.labelRes()),
                    selected = option == mode,
                    testTag = option.testTag(),
                    onSelect = { onModeChange(option) },
                )
            }

            HorizontalDivider()

            // « Affichage » plutôt que deux lignes commençant par « Afficher » : le titre de
            // section porte le verbe une fois pour toutes, et les items nomment simplement ce
            // qu'ils montrent. Même structure que « Mode de calcul » au-dessus.
            MenuHeading(stringResource(R.string.settings_display))
            SettingToggle(
                label = stringResource(R.string.settings_shadok_labels),
                checked = settings.showShadokLabels,
                testTag = SettingsTags.TOGGLE_LABELS,
                onCheckedChange = { onSettingsChange(settings.copy(showShadokLabels = it)) },
            )
            SettingToggle(
                label = stringResource(R.string.settings_decimal),
                checked = settings.showDecimal,
                testTag = SettingsTags.TOGGLE_DECIMAL,
                onCheckedChange = { onSettingsChange(settings.copy(showDecimal = it)) },
            )

            HorizontalDivider()

            // En pied de menu, et non en tête : on vient ici pour calculer, apprendre n'est
            // qu'occasionnel. Mais il faut que ce soit trouvable, sinon « ◿⅃ » ne veut rien
            // dire pour qui ouvre l'application la première fois.
            MenuHeading(stringResource(R.string.settings_help))
            DropdownMenuItem(
                text = { Text(stringResource(R.string.help_open)) },
                onClick = {
                    expanded = false
                    onOpenHelp()
                },
                modifier = Modifier.testTag(SettingsTags.HELP),
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.game_open)) },
                onClick = {
                    expanded = false
                    onOpenGame()
                },
                modifier = Modifier.testTag(SettingsTags.GAME),
            )
        }
    }
}

@Composable
private fun MenuHeading(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = HEADING_PADDING, vertical = HEADING_PADDING / 2),
    )
}

@Composable
private fun ModeChoice(label: String, selected: Boolean, testTag: String, onSelect: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onSelect,
        trailingIcon = { RadioButton(selected = selected, onClick = onSelect) },
        modifier = Modifier.testTag(testTag),
    )
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        // Le clic sur toute la ligne bascule le réglage : la cible est plus large que
        // l'interrupteur seul.
        onClick = { onCheckedChange(!checked) },
        trailingIcon = {
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        },
        modifier = Modifier.testTag(testTag),
    )
}

private fun CalculationMode.labelRes(): Int = when (this) {
    CalculationMode.SIMPLE -> R.string.mode_simple
    CalculationMode.CLASSIC -> R.string.mode_classic
    CalculationMode.RPN -> R.string.mode_rpn
}

private fun CalculationMode.testTag(): String = when (this) {
    CalculationMode.SIMPLE -> SettingsTags.MODE_SIMPLE
    CalculationMode.CLASSIC -> SettingsTags.MODE_CLASSIC
    CalculationMode.RPN -> SettingsTags.MODE_RPN
}

/** Aligné sur le retrait horizontal des `DropdownMenuItem` de Material 3. */
private val HEADING_PADDING = 16.dp
