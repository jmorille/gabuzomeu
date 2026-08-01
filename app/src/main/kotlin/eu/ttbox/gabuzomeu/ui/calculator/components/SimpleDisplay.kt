package eu.ttbox.gabuzomeu.ui.calculator.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.Operator
import eu.ttbox.gabuzomeu.ui.DisplayTags
import eu.ttbox.gabuzomeu.ui.calculator.CalculatorUiState
import eu.ttbox.gabuzomeu.ui.theme.DisplayTypography

/**
 * L'afficheur du mode Simple : une valeur, et l'opération qui l'attend.
 *
 * Une calculatrice à exécution immédiate ne montre qu'un nombre — c'est ce qui la rend
 * simple, et c'est aussi son piège. Après `Bu +`, l'écran montre toujours `Bu` : sans rien
 * d'autre, **appuyer sur `+` ne se verrait pas**. C'est exactement le défaut corrigé pour la
 * NPI en 2.0.0-RC5, où `6` tapé et `6` empilé se dessinaient au pixel près de la même façon.
 *
 * D'où le repère d'opération en attente, posé à gauche de la valeur et **hors de la zone
 * défilante** — même emplacement, et même raison, que le marqueur `≈` des lignes
 * d'affichage : un repère qu'un nombre trop long ferait disparaître ne servirait à rien.
 */
@Composable
internal fun ColumnScope.SimpleDisplay(
    state: CalculatorUiState,
    onPaste: (String) -> Unit,
    onCopied: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        PendingMarker(state.pending)
        Box(modifier = Modifier.weight(1f)) {
            ValueActions(state, onPaste, onCopied) {
                // Une frappe se suit par la queue, là où arrive le chiffre tapé ; une valeur
                // calculée se lit par la tête, ses rangs forts en premier.
                ValueLines(state, scrollToEnd = state.entering)
            }
        }
    }

    // La devise, tant que la machine est au repos. Elle explique la touche POMPER, et
    // s'efface au premier chiffre : à l'usage, elle ne prend la place de rien.
    if (state.isEmpty && state.pending == null) {
        Text(
            text = stringResource(R.string.simple_quote),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = QUOTE_SPACING),
        )
    }
}

/**
 * L'opération en attente, à gauche de la valeur.
 *
 * La colonne garde **toujours** sa largeur, même vide : sans cela, la grande valeur se
 * décalerait latéralement à chaque appui sur un opérateur, et ce mouvement se lirait comme
 * un changement de nombre — soit l'inverse de ce que le repère est censé dire.
 */
@Composable
private fun PendingMarker(pending: Operator?) {
    Box(
        modifier = Modifier
            .width(MARKER_COLUMN_WIDTH)
            .padding(end = MARKER_SPACING),
        contentAlignment = Alignment.Center,
    ) {
        if (pending == null) return@Box
        val name = stringResource(pending.labelRes())
        val description = stringResource(R.string.display_pending, name)

        Text(
            text = pending.symbol.toString(),
            style = DisplayTypography.labels,
            // `primary` et non le `tertiary` des opérateurs de la ligne de glyphes : ce
            // rôle-là est le sable de la marque, et un sable sur fond clair ne passe pas
            // le seuil de contraste. Un repère illisible ne repère rien.
            color = MaterialTheme.colorScheme.primary,
            // Lu par son nom — « Multiplié par, en attente » — et non par son symbole. Le
            // repère n'existe que quand l'opération existe : c'est ce qu'un test interroge.
            modifier = Modifier
                .testTag(DisplayTags.PENDING)
                .clearAndSetSemantics { contentDescription = description },
        )
    }
}

private fun Operator.labelRes(): Int = when (this) {
    Operator.PLUS -> R.string.key_plus
    Operator.MINUS -> R.string.key_minus
    Operator.TIMES -> R.string.key_times
    Operator.DIVIDE -> R.string.key_divide
}

/** Assez pour le plus large des quatre symboles, à la taille des noms Shadok. */
private val MARKER_COLUMN_WIDTH = 40.dp

private val MARKER_SPACING = 8.dp

/** La devise n'est pas une ligne d'affichage : elle se tient à distance. */
private val QUOTE_SPACING = 8.dp
