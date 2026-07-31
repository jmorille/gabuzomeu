package eu.ttbox.gabuzomeu.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

/**
 * Typographie du projet.
 *
 * Les tailles sont en `sp` — jamais en `dp` — pour honorer le réglage de taille de
 * police du système. Le projet d'origine mélangeait les deux
 * (`res/layout-port/main.xml:79,90` et `res/values/styles.xml:24,38` utilisaient `dp`
 * pour `textSize`), et employait même des `px` bruts dans `history_item.xml`.
 */
val GabuzomeuTypography = Typography()

/**
 * Styles propres à l'afficheur.
 *
 * Le **Shadok est la ligne principale** : c'est le sujet de l'application. Les noms
 * prononcés viennent ensuite, et la traduction décimale est secondaire.
 */
object DisplayTypography {

    /** Ligne principale : les glyphes Shadok. */
    val glyphs = TextStyle(
        fontSize = 44.sp,
        fontWeight = FontWeight.Normal,
        textAlign = TextAlign.End,
    )

    /** Deuxième ligne : les noms prononcés, `BuZo`. */
    val labels = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.End,
    )

    /** Ligne secondaire : la traduction décimale. */
    val decimal = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        // Chiffres à largeur fixe : l'expression ne « danse » pas pendant la frappe.
        fontFamily = FontFamily.Monospace,
        textAlign = TextAlign.End,
    )

    /** Touches du pavé. */
    val key = TextStyle(
        fontSize = 26.sp,
        fontWeight = FontWeight.Medium,
    )
}
