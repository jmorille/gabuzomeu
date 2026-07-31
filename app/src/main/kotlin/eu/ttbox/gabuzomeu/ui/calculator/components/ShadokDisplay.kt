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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.ui.DisplayTags
import eu.ttbox.gabuzomeu.ui.calculator.CalculatorUiState
import eu.ttbox.gabuzomeu.ui.calculator.StackLevel
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
fun ShadokDisplay(
    state: CalculatorUiState,
    modifier: Modifier = Modifier,
    onPaste: (String) -> Unit = {},
    onCopied: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.mode == CalculationMode.RPN) {
            RpnStackArea(
                levels = state.stack,
                showDecimal = state.settings.showDecimal,
                notation = state.notation,
                onCopied = onCopied,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            HorizontalDivider()
            // X reprend la disposition des niveaux de pile — décimal à gauche, Shadok à
            // droite — pour que l'œil lise la colonne des décimales d'un seul trait, du
            // fond de pile jusqu'au calcul en cours.
            ValueActions(state, onPaste, onCopied) { XValue(state) }
        } else {
            ValueActions(state, onPaste, onCopied) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    GlyphLine(state)
                    if (state.settings.showShadokLabels) LabelLine(state)
                    if (state.settings.showDecimal) DecimalLine(state)
                }
            }
        }

        state.error?.let { error ->
            Text(
                text = stringResource(error.messageRes()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

/**
 * La valeur principale, rendue copiable.
 *
 * Un seul point d'ancrage pour les deux modes : en NPI c'est X, en classique l'expression
 * entière — dans les deux cas, ce que l'utilisateur désigne quand il appuie sur l'afficheur.
 */
@Composable
private fun ValueActions(
    state: CalculatorUiState,
    onPaste: (String) -> Unit,
    onCopied: () -> Unit,
    content: @Composable () -> Unit,
) {
    DisplayActions(
        value = CopyableValue.of(state),
        notation = state.notation,
        tag = DisplayTags.ACTIONS,
        modifier = Modifier.fillMaxWidth(),
        onPaste = onPaste,
        onCopied = onCopied,
        content = content,
    )
}

/**
 * Le registre X en NPI : décimal et glyphes **sur la même ligne**, les noms en dessous.
 *
 * La première ligne reprend exactement le partage des niveaux de pile — décimal à gauche,
 * Shadok à droite, mêmes moitiés — si bien que les deux colonnes se lisent d'un seul trait
 * du fond de pile jusqu'au calcul en cours. Les noms prononcés viennent ensuite, sur toute
 * la largeur : ce sont eux qui s'allongent le plus (`MeuZoGa` pour trois chiffres).
 *
 * En mode classique l'afficheur reste empilé : il n'y a pas de pile au-dessus avec laquelle
 * s'aligner, et une expression entière a besoin de toute la largeur.
 */
@Composable
private fun XValue(state: CalculatorUiState) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(VALUE_COLUMN_SPACING),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.settings.showDecimal) {
                XDecimal(state, modifier = Modifier.weight(1f))
            }
            GlyphLine(state, modifier = Modifier.weight(1f))
        }
        // Le « ≈ » du Shadok est déjà porté par la ligne de glyphes : le répéter sur les
        // noms le ferait paraître deux fois pour une seule et même valeur.
        if (state.settings.showShadokLabels) LabelLine(state, showApproximation = false)
    }
}

/** Le décimal de X, aligné à gauche et son marqueur `≈` ancré hors du défilement. */
@Composable
private fun XDecimal(state: CalculatorUiState, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        if (state.decimalApproximate) {
            Text(
                text = ShadokFormatter.APPROXIMATION.toString(),
                style = DisplayTypography.decimal,
                color = color,
                modifier = Modifier.padding(end = 4.dp),
            )
        }
        AnimatedContent(
            targetState = state.decimal,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "decimal-x",
            modifier = Modifier.weight(1f),
        ) { text ->
            Text(
                text = text,
                style = DisplayTypography.decimal,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .testTag(DisplayTags.DECIMAL)
                    .fillMaxWidth()
                    .horizontalScroll(state = rememberScrollState(), reverseScrolling = true),
            )
        }
    }
}

/** Ligne principale : les glyphes Shadok, l'identité de l'application. */
@Composable
private fun GlyphLine(state: CalculatorUiState, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurface
    DisplayLine(
        approximate = state.shadokApproximate,
        style = DisplayTypography.glyphs,
        color = color,
        modifier = modifier,
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
private fun LabelLine(state: CalculatorUiState, showApproximation: Boolean = true) {
    val color = MaterialTheme.colorScheme.primary
    DisplayLine(
        approximate = state.shadokApproximate && showApproximation,
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
    modifier: Modifier = Modifier,
    content: @Composable (Modifier) -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
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
    EvalError.STACK_UNDERFLOW -> R.string.error_stack_underflow
}
