package eu.ttbox.gabuzomeu.ui.shadok

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit

/**
 * Affiche une expression en glyphes Shadok, opérateurs compris.
 *
 * Les chiffres sont des dessins vectoriels, les opérateurs du texte : le mélange se fait
 * par `InlineTextContent`, ce qui laisse le moteur de texte gérer alignement, retour à
 * la ligne et mise à l'échelle avec la taille de police du système.
 *
 * @param semanticsLabel ce que doit lire TalkBack — les **noms** Shadok, pas les formes.
 *   C'est ici que le bug d'accessibilité du projet d'origine devient impossible : les
 *   descriptions Zo/Meu y étaient inversées dans `res/layout-port/shadok_pad.xml:32-42`
 *   parce qu'elles étaient recopiées à la main dans le XML, alors qu'elles dérivent
 *   désormais toujours de [ShadokDigit.label].
 */
@Composable
fun ShadokGlyphText(
    expression: String,
    semanticsLabel: String,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val glyphSize = style.fontSize

    val inlineContent = remember(glyphSize, color) {
        ShadokDigit.entries.associate { digit ->
            inlineIdOf(digit) to InlineTextContent(
                placeholder = Placeholder(
                    width = glyphSize,
                    height = glyphSize,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Box(modifier = Modifier) {
                    Icon(
                        imageVector = ShadokGlyphs.of(digit),
                        // La description est portée par le conteneur entier.
                        contentDescription = null,
                        tint = color,
                    )
                }
            }
        }
    }

    val annotated = remember(expression) {
        buildAnnotatedString {
            expression.forEach { character ->
                val digit = ShadokDigit.ofGlyphOrNull(character)
                if (digit == null) {
                    append(character)
                } else {
                    // Le second argument est le texte de repli si le contenu inline
                    // n'est pas résolu — on y met le glyphe lui-même.
                    appendInlineContent(inlineIdOf(digit), digit.glyph.toString())
                }
            }
        }
    }

    Text(
        text = annotated,
        inlineContent = inlineContent,
        style = style,
        color = color,
        modifier = modifier.clearAndSetSemantics { contentDescription = semanticsLabel },
    )
}

private fun inlineIdOf(digit: ShadokDigit): String = "shadok-${digit.name}"
