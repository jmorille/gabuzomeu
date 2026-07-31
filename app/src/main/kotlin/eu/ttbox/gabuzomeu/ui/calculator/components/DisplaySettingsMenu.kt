package eu.ttbox.gabuzomeu.ui.calculator.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.data.DisplaySettings
import eu.ttbox.gabuzomeu.ui.SettingsTags

/**
 * Réglages d'affichage : masquer les noms Shadok, masquer la traduction décimale.
 *
 * La ligne de glyphes n'est pas proposée : elle est le sujet de l'application, la
 * masquer viderait l'écran de son sens.
 */
@Composable
fun DisplaySettingsMenu(
    settings: DisplaySettings,
    onSettingsChange: (DisplaySettings) -> Unit,
    modifier: Modifier = Modifier,
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
            SettingToggle(
                label = stringResource(R.string.settings_show_shadok_labels),
                checked = settings.showShadokLabels,
                testTag = SettingsTags.TOGGLE_LABELS,
                onCheckedChange = {
                    onSettingsChange(settings.copy(showShadokLabels = it))
                },
            )
            SettingToggle(
                label = stringResource(R.string.settings_show_decimal),
                checked = settings.showDecimal,
                testTag = SettingsTags.TOGGLE_DECIMAL,
                onCheckedChange = {
                    onSettingsChange(settings.copy(showDecimal = it))
                },
            )
        }
    }
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
