package eu.ttbox.gabuzomeu.core.eval

/**
 * Les quatre opérateurs de la calculatrice.
 *
 * Les symboles affichés sont les vrais caractères mathématiques, comme dans le projet
 * d'origine (`CalculatorEditable.java:23-24`) : `−` U+2212, `×` U+00D7, `÷` U+00F7.
 * Les formes ASCII tapées au clavier (`-`, `*`, `/`) sont acceptées en entrée puis
 * substituées.
 *
 * Le pavé scientifique (sin, cos, ln, π, factorielle…) a été abandonné : il reposait
 * sur la bibliothèque `arity`, un jar jamais publié sur un dépôt public et livré à la
 * main dans le projet. Le périmètre se limite donc à l'arithmétique.
 */
enum class Operator(val symbol: Char, val precedence: Int) {
    PLUS('+', precedence = 1),
    MINUS('−', precedence = 1),
    TIMES('×', precedence = 2),
    DIVIDE('÷', precedence = 2),
    ;

    companion object {
        private val bySymbol: Map<Char, Operator> = buildMap {
            // Operator.entries explicitement : dans buildMap, « entries » seul
            // désignerait celui de la map en construction.
            Operator.entries.forEach { operator -> put(operator.symbol, operator) }
            // Formes ASCII saisies au clavier physique.
            put('-', MINUS)
            put('*', TIMES)
            put('/', DIVIDE)
        }

        fun ofSymbolOrNull(symbol: Char): Operator? = bySymbol[symbol]

        fun isOperator(symbol: Char): Boolean = bySymbol.containsKey(symbol)
    }
}
