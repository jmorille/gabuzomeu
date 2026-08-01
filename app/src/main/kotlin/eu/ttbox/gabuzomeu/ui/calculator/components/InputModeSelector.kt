package eu.ttbox.gabuzomeu.ui.calculator.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.ui.NotationTags

/**
 * Choix du mode de saisie : décimal ou Shadok.
 *
 * Remplace le `ViewPager` du projet d'origine. Un balayage horizontal entre panneaux
 * était invisible pour qui ne le connaissait pas, et inaccessible à TalkBack ; un
 * sélecteur explicite se voit, s'annonce, et se pilote au clavier.
 */
@Composable
fun InputModeSelector(
    notation: NumberNotation,
    onNotationChange: (NumberNotation) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = NumberNotation.entries

    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == notation,
                onClick = { onNotationChange(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                // Un repère, et non le libellé : « Décimal » nomme aussi un réglage du menu,
                // et deux nœuds au même texte rendaient la recherche par mot ambiguë.
                modifier = Modifier.testTag(NotationTags.of(option)),
            ) {
                Text(text = stringResource(option.labelRes()))
            }
        }
    }
}

private fun NumberNotation.labelRes(): Int = when (this) {
    NumberNotation.DECIMAL -> R.string.mode_decimal
    NumberNotation.SHADOK -> R.string.mode_shadok
}
