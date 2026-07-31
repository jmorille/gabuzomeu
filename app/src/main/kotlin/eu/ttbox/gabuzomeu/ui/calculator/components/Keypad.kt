package eu.ttbox.gabuzomeu.ui.calculator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.ui.KeypadTags
import eu.ttbox.gabuzomeu.ui.calculator.KeyAction
import eu.ttbox.gabuzomeu.ui.shadok.ShadokGlyphs
import eu.ttbox.gabuzomeu.ui.theme.DisplayTypography

/**
 * Le pavé de touches.
 *
 * Remplace à lui seul `ColorButton` (qui castait son `Context` en `Calculator`, donc
 * inutilisable ailleurs), `EventListener` (qui routait les appuis **par le texte du
 * bouton**), et les huit fichiers de disposition XML par orientation.
 */
@Composable
fun Keypad(
    mode: CalculationMode,
    notation: NumberNotation,
    onKey: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = KeypadLayout.forMode(mode, notation)

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(KEY_SPACING),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(KEY_SPACING),
            ) {
                row.forEach { key ->
                    KeyButton(
                        key = key,
                        onKey = onKey,
                        modifier = Modifier
                            .weight(key.weight)
                            .fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun KeyButton(key: KeySpec, onKey: (KeyAction) -> Unit, modifier: Modifier = Modifier) {
    val description = when {
        key.glyph != null -> key.glyph.label
        key.contentDescriptionRes != null -> stringResource(key.contentDescriptionRes)
        else -> key.text.orEmpty()
    }

    // clearAndSetSemantics : TalkBack annonce « Zo », jamais la forme « ⅃ ».
    //
    // Conséquence à connaître : cela efface aussi la sémantique de texte des
    // descendants, donc une touche n'est PAS trouvable par `onNodeWithText`. D'où le
    // testTag, posé dans le même bloc — un repère stable, indépendant de la langue.
    val semantics = Modifier.clearAndSetSemantics {
        contentDescription = description
        testTag = KeypadTags.of(key.action)
    }

    val content: @Composable () -> Unit = {
        if (key.glyph != null) {
            Icon(
                imageVector = ShadokGlyphs.of(key.glyph),
                contentDescription = null,
                modifier = Modifier.size(GLYPH_KEY_SIZE),
            )
        } else {
            Text(
                text = key.text.orEmpty(),
                style = DisplayTypography.key,
                maxLines = 1,
                // La touche s'adapte au libellé plutôt que de le rogner : « ENTER ↵ » ne
                // tient pas à la taille nominale sur une touche étroite, et le repli
                // protège tous les libellés quand la police du système est agrandie.
                autoSize = TextAutoSize.StepBased(
                    minFontSize = MIN_KEY_FONT_SIZE,
                    maxFontSize = DisplayTypography.key.fontSize,
                ),
            )
        }
    }

    Button(
        onClick = { onKey(key.action) },
        modifier = modifier.then(semantics),
        colors = key.kind.colors(),
        contentPadding = ButtonDefaults.TextButtonContentPadding,
    ) { content() }
}

/**
 * Couleurs d'une touche.
 *
 * Conteneur **et** contenu sont toujours déclarés ensemble, en respectant les couples de
 * rôles Material 3 (`xContainer` / `onXContainer`). Ne fixer que `containerColor` laisse
 * `contentColor` à sa valeur par défaut, celle d'un *autre* rôle : le résultat est un
 * symbole quasi illisible sur son fond — c'était le cas de la rangée de fonctions en
 * thème sombre, verdâtre sur lavande.
 */
@Composable
private fun KeyKind.colors(): ButtonColors = when (this) {
    KeyKind.EQUALS -> ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
    )

    KeyKind.OPERATOR -> ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    )

    KeyKind.FUNCTION -> ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
    )

    KeyKind.DIGIT -> ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
}

private val KEY_SPACING = 8.dp
private val GLYPH_KEY_SIZE = 30.dp

/** Plancher du repli typographique : en dessous, une touche cesse d'être lisible. */
private val MIN_KEY_FONT_SIZE = 12.sp
