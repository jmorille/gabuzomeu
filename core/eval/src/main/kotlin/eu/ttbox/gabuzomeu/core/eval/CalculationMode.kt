package eu.ttbox.gabuzomeu.core.eval

/**
 * Comment l'utilisateur exprime un calcul.
 *
 * Axe **orthogonal** à [NumberNotation] : on choisit indépendamment la façon d'écrire les
 * nombres (décimal ou Shadok) et la façon d'enchaîner les opérations. Les six combinaisons
 * ont un sens, d'où les six dispositions de pavé.
 *
 * L'ordre de déclaration est celui du menu, et c'est un ordre de difficulté croissante. Rien
 * ne dépend de l'ordinal : la persistance stocke le `name`, donc insérer un mode en tête ne
 * dérange aucune session existante.
 */
enum class CalculationMode {

    /**
     * Exécution immédiate : `Bu + Zo =`, comme une calculatrice de poche.
     *
     * Chaque opérateur résout le précédent au lieu de l'accumuler — il n'y a donc **ni
     * priorité ni parenthèses**, et `Bu + Zo × Meu =` vaut 9 là où [CLASSIC] donne 7. Ce
     * n'est pas une version bridée du mode classique mais une autre machine, celle que
     * dessine l'affiche des Shadoks : quatre chiffres, quatre opérateurs, POMPER et ÉGAL.
     *
     * Modélisée par [SimpleSession].
     */
    SIMPLE,

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
