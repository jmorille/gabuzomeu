package eu.ttbox.gabuzomeu.ui.calculator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.ui.calculator.CalculatorUiState
import eu.ttbox.gabuzomeu.ui.shadok.ShadokGlyphText
import eu.ttbox.gabuzomeu.ui.theme.DisplayTypography

/**
 * Les trois écritures simultanées : décimal, glyphes Shadok, noms Shadok.
 *
 * Contrairement au projet d'origine, ce ne sont pas trois champs éditables synchronisés
 * entre eux mais trois projections en lecture seule du même tampon. Tout l'échafaudage
 * de `CalculatorEditText` (256 lignes : suppression de l'IME, `NoTextSelectionMode`,
 * menu contextuel copier/coller maison) et de `CalculatorDisplay` (`ViewSwitcher` +
 * `TranslateAnimation`) disparaît : sans champ de saisie, il n'y a plus de clavier
 * système à combattre.
 */
@Composable
fun TripleDisplay(state: CalculatorUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DecimalLine(state)
        GlyphLine(state)
        LabelLine(state)

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Ligne principale : le décimal, la notation de référence. */
@Composable
private fun DecimalLine(state: CalculatorUiState) {
    val color = MaterialTheme.colorScheme.onSurface
    DisplayLine(
        approximate = state.decimalApproximate,
        style = DisplayTypography.primary,
        color = color,
    ) { lineModifier ->
        AnimatedContent(
            targetState = state.decimal,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "decimal",
            modifier = lineModifier,
        ) { text ->
            ScrollingText(text = text, style = DisplayTypography.primary, color = color)
        }
    }
}

/** Ligne des glyphes, dessinés en vectoriel. */
@Composable
private fun GlyphLine(state: CalculatorUiState) {
    val color = MaterialTheme.colorScheme.primary
    DisplayLine(
        approximate = state.shadokApproximate,
        style = DisplayTypography.glyphs,
        color = color,
    ) { lineModifier ->
        ShadokGlyphText(
            expression = state.glyphs,
            // TalkBack lit les noms, pas les formes.
            semanticsLabel = state.labels.ifEmpty { stringResource(R.string.display_empty) },
            style = DisplayTypography.glyphs,
            color = color,
            modifier = lineModifier.horizontalScroll(
                state = rememberScrollState(),
                reverseScrolling = true,
            ),
        )
    }
}

/** Ligne des noms prononcés : « Quand il y a encore un shadok de plus… » */
@Composable
private fun LabelLine(state: CalculatorUiState) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    DisplayLine(
        approximate = state.shadokApproximate,
        style = DisplayTypography.labels,
        color = color,
    ) { lineModifier ->
        ScrollingText(
            text = state.labels,
            style = DisplayTypography.labels,
            color = color,
            modifier = lineModifier,
        )
    }
}

/**
 * Une ligne d'affichage, avec son marqueur d'approximation **hors** de la zone défilante.
 *
 * C'est le point important : les lignes défilent vers leur fin (`reverseScrolling`) pour
 * montrer les derniers chiffres saisis. Un `≈` placé en tête du texte lui-même sortirait
 * donc de l'écran dès que l'expression dépasse la largeur — et un marqueur invisible ne
 * sert à rien. Ancré ici dans un `Row`, il reste toujours visible.
 */
@Composable
private fun DisplayLine(
    approximate: Boolean,
    style: TextStyle,
    color: Color,
    content: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (approximate) {
            Text(
                text = ShadokFormatter.APPROXIMATION.toString(),
                style = style,
                color = color,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        content(Modifier.weight(1f))
    }
}

@Composable
private fun ScrollingText(
    text: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        textAlign = TextAlign.End,
        modifier = modifier
            .fillMaxWidth()
            // Une expression longue défile au lieu d'être tronquée.
            .horizontalScroll(state = rememberScrollState(), reverseScrolling = true),
    )
}

private fun EvalError.messageRes(): Int = when (this) {
    EvalError.EMPTY -> R.string.error_empty
    EvalError.SYNTAX -> R.string.error_syntax
    EvalError.UNBALANCED_PARENTHESES -> R.string.error_parentheses
    EvalError.DIVISION_BY_ZERO -> R.string.error_division_by_zero
}
