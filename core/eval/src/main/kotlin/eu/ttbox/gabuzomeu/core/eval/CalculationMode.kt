package eu.ttbox.gabuzomeu.core.eval

/**
 * Comment l'utilisateur exprime un calcul.
 *
 * Axe **orthogonal** à [NumberNotation] : on choisit indépendamment la façon d'écrire les
 * nombres (décimal ou Shadok) et la façon d'enchaîner les opérations. Les quatre
 * combinaisons ont un sens, d'où les quatre dispositions de pavé.
 */
enum class CalculationMode {

    /**
     * Notation infixe : `6×7=`, avec parenthèses et priorité des opérateurs.
     *
     * Modélisée par [ExpressionBuffer] et calculée par [Evaluator].
     */
    CLASSIC,

    /**
     * Notation polonaise inverse, dite postfixe : `6 ENTER 7 ×`.
     *
     * Les opérandes s'empilent, l'opérateur s'applique aussitôt aux deux valeurs du
     * sommet. Ni parenthèses ni priorité — l'ordre de frappe *est* l'ordre de calcul.
     * Modélisée par [RpnSession] ; il n'y a rien à parser, donc pas d'évaluateur.
     */
    RPN,
}
