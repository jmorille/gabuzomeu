package eu.ttbox.gabuzomeu.ui.calculator

import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.data.DisplaySettings

/**
 * Un niveau de la pile NPI, dans ses écritures affichables.
 *
 * Les noms Shadok y figurent bien qu'aucune ligne de pile ne les montre aujourd'hui :
 * c'est ce que lit TalkBack. Un niveau annoncé « barre bas, L inversé » ne dirait rien —
 * il s'annonce « BuZo ».
 */
data class StackLevel(
    val glyphs: String,
    val labels: String,
    val decimal: String,
    /** Ni affichée ni lue : uniquement ce qu'un appui long copie. */
    val base4: String = "",
    val shadokApproximate: Boolean = false,
    val decimalApproximate: Boolean = false,
)

/**
 * L'état affichable de la calculatrice — immuable, dérivé du mode actif.
 *
 * Les trois chaînes sont trois projections de la **même** valeur, calculées d'un coup à
 * chaque changement. Le code d'origine maintenait trois `EditText` synchronisés l'un depuis
 * l'autre par conversions successives, avec les pertes d'information que cela suppose.
 *
 * En NPI, elles décrivent le registre X — la frappe en cours, ou à défaut le sommet de la
 * pile — et [stack] porte les niveaux situés en dessous.
 */
data class CalculatorUiState(
    val mode: CalculationMode = CalculationMode.CLASSIC,
    val notation: NumberNotation = NumberNotation.DECIMAL,
    val glyphs: String = "",
    val labels: String = "",
    val decimal: String = "",
    /**
     * La même valeur en chiffres base 4 bruts — `12` pour 6.
     *
     * Aucune ligne ne la montre : elle existe pour le presse-papiers, parce que `12` se colle
     * et se relit partout, là où `_⅃` dépend d'une police qui porte U+2143.
     */
    val base4: String = "",
    /** Au moins un nombre a dû être tronqué pour s'écrire en base 4. */
    val shadokApproximate: Boolean = false,
    /** Valeur non représentable exactement en décimal (un tiers, par exemple). */
    val decimalApproximate: Boolean = false,
    /** Les niveaux sous X, du fond de pile vers X. Vide hors NPI. */
    val stack: List<StackLevel> = emptyList(),
    val error: EvalError? = null,
    /** L'affichage montre le résultat d'un « = » et non une saisie en cours. */
    val showingResult: Boolean = false,
    val settings: DisplaySettings = DisplaySettings(),
) {
    val isEmpty: Boolean get() = decimal.isEmpty()
}
