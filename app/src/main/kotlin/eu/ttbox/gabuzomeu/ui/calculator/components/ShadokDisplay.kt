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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.ui.DisplayTags
import eu.ttbox.gabuzomeu.ui.calculator.CalculatorUiState
import eu.ttbox.gabuzomeu.ui.shadok.ShadokGlyphText
import eu.ttbox.gabuzomeu.ui.shadok.ShadokLabelText
import eu.ttbox.gabuzomeu.ui.theme.DisplayTypography

/**
 * L'afficheur : les glyphes Shadok en vedette, puis les noms, puis le décimal.
 *
 * Contrairement au projet d'origine, ce ne sont pas des champs éditables synchronisés
 * entre eux mais des projections en lecture seule du même tampon. Tout l'échafaudage de
 * `CalculatorEditText` (256 lignes : suppression de l'IME, `NoTextSelectionMode`, menu
 * contextuel copier/coller maison) et de `CalculatorDisplay` (`ViewSwitcher` +
 * `TranslateAnimation`) disparaît : sans champ de saisie, il n'y a plus de clavier
 * système à combattre.
 *
 * Les deux lignes secondaires se masquent depuis les réglages ; la ligne de glyphes,
 * elle, est toujours là.
 */
@Composable
fun ShadokDisplay(state: CalculatorUiState, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GlyphLine(state)
        if (state.settings.showShadokLabels) LabelLine(state)
        if (state.settings.showDecimal) DecimalLine(state)

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/** Ligne principale : les glyphes Shadok, l'identité de l'application. */
@Composable
private fun GlyphLine(state: CalculatorUiState) {
    val color = MaterialTheme.colorScheme.onSurface
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
            operatorColor = MaterialTheme.colorScheme.tertiary,
            modifier = lineModifier
                .testTag(DisplayTags.GLYPHS)
                .horizontalScroll(state = rememberScrollState(), reverseScrolling = true),
        )
    }
}

/** Deuxième ligne : les noms prononcés — « quand il y a encore un shadok de plus… ». */
@Composable
private fun LabelLine(state: CalculatorUiState) {
    val color = MaterialTheme.colorScheme.primary
    DisplayLine(
        approximate = state.shadokApproximate,
        style = DisplayTypography.labels,
        color = color,
    ) { lineModifier ->
        ShadokLabelText(
            expression = state.labels,
            style = DisplayTypography.labels,
            color = color,
            operatorColor = MaterialTheme.colorScheme.tertiary,
            modifier = lineModifier
                .testTag(DisplayTags.LABELS)
                .fillMaxWidth()
                .horizontalScroll(state = rememberScrollState(), reverseScrolling = true),
        )
    }
}

/** Ligne secondaire : la traduction décimale. */
@Composable
private fun DecimalLine(state: CalculatorUiState) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    DisplayLine(
        approximate = state.decimalApproximate,
        style = DisplayTypography.decimal,
        color = color,
    ) { lineModifier ->
        AnimatedContent(
            targetState = state.decimal,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "decimal",
            modifier = lineModifier,
        ) { text ->
            Text(
                text = text,
                style = DisplayTypography.decimal,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .testTag(DisplayTags.DECIMAL)
                    .fillMaxWidth()
                    .horizontalScroll(state = rememberScrollState(), reverseScrolling = true),
            )
        }
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

private fun EvalError.messageRes(): Int = when (this) {
    EvalError.EMPTY -> R.string.error_empty
    EvalError.SYNTAX -> R.string.error_syntax
    EvalError.UNBALANCED_PARENTHESES -> R.string.error_parentheses
    EvalError.DIVISION_BY_ZERO -> R.string.error_division_by_zero
}
