package eu.ttbox.gabuzomeu.ui.calculator

import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.eval.NumberNotation

/**
 * L'état affichable de la calculatrice — immuable, dérivé du tampon d'expression.
 *
 * Les trois chaînes sont trois projections de la **même** expression, calculées d'un
 * coup à chaque changement. Le code d'origine maintenait trois `EditText` synchronisés
 * l'un depuis l'autre par conversions successives, avec les pertes d'information que
 * cela suppose.
 */
data class CalculatorUiState(
    val notation: NumberNotation = NumberNotation.DECIMAL,
    val decimal: String = "",
    val glyphs: String = "",
    val labels: String = "",
    /** Au moins un nombre a dû être tronqué pour s'écrire en base 4. */
    val shadokApproximate: Boolean = false,
    /** Résultat non représentable exactement en décimal (un tiers, par exemple). */
    val decimalApproximate: Boolean = false,
    val error: EvalError? = null,
    /** L'affichage montre le résultat d'un « = » et non une saisie en cours. */
    val showingResult: Boolean = false,
) {
    val isEmpty: Boolean get() = decimal.isEmpty()
}
