package eu.ttbox.gabuzomeu.core.eval

import eu.ttbox.gabuzomeu.core.shadok.Rational

/**
 * La pile de la calculatrice en notation polonaise inverse.
 *
 * Immuable, comme [ExpressionBuffer] : chaque opération renvoie une nouvelle pile. C'est
 * ce qui permet à [RpnSession] de refuser une opération en renvoyant simplement l'état
 * d'avant, sans avoir à défaire quoi que ce soit.
 *
 * Les valeurs sont des [Rational] exacts : `1 ENTER 3 ÷` empile un vrai tiers, que la
 * base 4 sait ensuite rendre comme le développement périodique `Ga.BuBuBu…`.
 *
 * @property values les valeurs empilées, **le sommet en dernier** — même convention que
 *   `ExpressionBuffer.atoms.lastOrNull()` pour l'atome courant. La profondeur n'est pas
 *   bornée : contrairement aux HP à quatre registres X/Y/Z/T, rien ici n'oblige à
 *   perdre le fond de pile.
 */
data class RpnStack(val values: List<Rational> = emptyList()) {

    val depth: Int get() = values.size

    val isEmpty: Boolean get() = values.isEmpty()

    /** La valeur du sommet, celle sur laquelle agissent [dropTop] et [negateTop]. */
    val top: Rational? get() = values.lastOrNull()

    fun push(value: Rational): RpnStack = RpnStack(values + value)

    /** Dépile le sommet. Une pile vide reste vide : dépiler n'est pas une erreur. */
    fun dropTop(): RpnStack = if (isEmpty) this else RpnStack(values.dropLast(1))

    /** Échange les deux valeurs du sommet, ou `null` si elles n'y sont pas toutes deux. */
    fun swapTop(): RpnStack? {
        if (depth < BINARY_ARITY) return null
        val reordered = values.toMutableList()
        val last = reordered.size - 1
        reordered[last] = values[last - 1]
        reordered[last - 1] = values[last]
        return RpnStack(reordered)
    }

    /** Change le signe du sommet. Sans effet sur une pile vide. */
    fun negateTop(): RpnStack {
        val summit = top ?: return this
        return RpnStack(values.dropLast(1) + -summit)
    }

    fun clear(): RpnStack = RpnStack()

    /**
     * Applique un opérateur binaire aux deux valeurs du sommet, qu'il remplace par le
     * résultat.
     *
     * L'ordre compte : le sommet est l'opérande **droit**, celui d'en dessous le gauche.
     * `10 ENTER 3 −` vaut donc 7 et non −7, comme sur n'importe quelle HP.
     *
     * @return `null` si la pile compte moins de deux valeurs.
     * @throws ArithmeticException sur une division par zéro — levée par [Rational.div] et
     *   rattrapée par [RpnSession], qui rend alors la pile intacte.
     */
    fun apply(operator: Operator): RpnStack? {
        if (depth < BINARY_ARITY) return null
        val right = values[depth - 1]
        val left = values[depth - 2]
        val result = when (operator) {
            Operator.PLUS -> left + right
            Operator.MINUS -> left - right
            Operator.TIMES -> left * right
            Operator.DIVIDE -> left / right
        }
        return RpnStack(values.dropLast(BINARY_ARITY) + result)
    }

    /**
     * L'écriture de persistance : les fractions séparées par des `;`, du fond au sommet.
     *
     * On stocke `Rational.toString()` — `"7"` ou `"-1/3"` — et non le rendu décimal :
     * l'exactitude survit ainsi au redémarrage.
     */
    fun keys(): String = values.joinToString(separator = SEPARATOR.toString())

    companion object {
        /** Nos quatre opérateurs sont tous binaires : deux valeurs requises. */
        private const val BINARY_ARITY = 2

        private const val SEPARATOR = ';'

        /** Relit [keys]. Les fragments illisibles sont ignorés plutôt que fatals. */
        fun restore(keys: String): RpnStack = RpnStack(
            keys.split(SEPARATOR)
                .filter { it.isNotBlank() }
                .mapNotNull(Rational::parseOrNull),
        )
    }
}
