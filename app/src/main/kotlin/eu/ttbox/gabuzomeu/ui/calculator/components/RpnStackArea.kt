package eu.ttbox.gabuzomeu.ui.calculator.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.ui.DisplayTags
import eu.ttbox.gabuzomeu.ui.calculator.StackLevel
import eu.ttbox.gabuzomeu.ui.shadok.ShadokGlyphText
import eu.ttbox.gabuzomeu.ui.theme.DisplayTypography

/**
 * Écart entre la colonne des décimales et celle des Shadok.
 *
 * Partagé par les niveaux de pile et par la ligne X d'[ShadokDisplay] : c'est ce qui fait
 * que les deux colonnes se lisent d'un seul trait de part et d'autre du séparateur.
 */
internal val VALUE_COLUMN_SPACING = 12.dp

/**
 * La pile NPI : les niveaux **sous** X, le plus proche de X en bas.
 *
 * `reverseLayout` fait deux choses d'un coup : le sommet se colle au séparateur, juste
 * au-dessus de la ligne principale, et la liste reste ancrée en bas quand la pile grandit —
 * un empilement de dix valeurs ne pousse pas le calcul en cours hors de l'écran. Les
 * niveaux profonds défilent vers le haut, sans limite de profondeur.
 *
 * Chaque niveau porte ses glyphes et son décimal, mais pas ses noms : trois écritures par
 * ligne saturerait la hauteur, et c'est X qui a besoin du détail complet.
 *
 * @param showDecimal le réglage d'affichage du décimal. Il vaut pour toute la colonne, pile
 *   comprise : masquer le décimal sur X et le laisser sur les niveaux au-dessus donnerait
 *   un afficheur mi-figue mi-raisin.
 */
@Composable
internal fun RpnStackArea(
    levels: List<StackLevel>,
    showDecimal: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.testTag(DisplayTags.STACK),
        reverseLayout = true,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        // La liste est inversée comme la disposition : l'indice 0 est donc le niveau
        // immédiatement sous X — celui que les HP appellent Y.
        itemsIndexed(levels.asReversed()) { depth, level ->
            StackRow(level = level, depth = depth, showDecimal = showDecimal)
        }
    }
}

/** Un niveau de pile : sa valeur décimale à gauche, ses glyphes à droite. */
@Composable
private fun StackRow(level: StackLevel, depth: Int, showDecimal: Boolean) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    // « Pile, niveau 1 : BuZo » — le rang et les noms Shadok. Les formes ne se prononcent
    // pas, et répéter le décimal ferait doublon avec la ligne principale.
    val description = stringResource(R.string.display_stack_level, depth + 1, level.labels)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Même précaution que sur les touches : clearAndSetSemantics efface la
            // sémantique des descendants, donc le testTag doit être posé dans le bloc.
            .clearAndSetSemantics {
                contentDescription = description
                testTag = DisplayTags.stackLevel(depth)
            },
        horizontalArrangement = Arrangement.spacedBy(VALUE_COLUMN_SPACING),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Moitiés égales, ici comme sur la ligne X : les décimales s'alignent en colonne
        // du fond de pile jusqu'au calcul en cours, et les glyphes de même à droite.
        // Décimal masqué, les glyphes prennent toute la largeur.
        if (showDecimal) {
            Text(
                text = level.decimalWithMarker(),
                style = DisplayTypography.decimal,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(state = rememberScrollState(), reverseScrolling = true),
            )
        }
        ShadokGlyphText(
            expression = level.glyphs,
            semanticsLabel = level.labels,
            style = DisplayTypography.labels,
            color = MaterialTheme.colorScheme.onSurface,
            operatorColor = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(state = rememberScrollState(), reverseScrolling = true),
        )
    }
}

/**
 * Le décimal d'un niveau, précédé de `≈` s'il est tronqué.
 *
 * Contrairement aux lignes principales, le marqueur est ici dans le texte : une ligne de
 * pile est courte, il ne risque pas de sortir de l'écran.
 */
private fun StackLevel.decimalWithMarker(): String =
    if (decimalApproximate) "${ShadokFormatter.APPROXIMATION}$decimal" else decimal
