package eu.ttbox.gabuzomeu.core.eval

/** Levée en interne par [Parser] ; convertie en [EvalResult.Failure] par l'évaluateur. */
internal class ParseException(val error: EvalError) : Exception(error.name)

/**
 * Analyseur à descente récursive.
 *
 * ```
 * expression := terme      (('+' | '−') terme)*
 * terme      := facteur    (('×' | '÷') facteur)*
 * facteur    := '−' facteur | primaire
 * primaire   := nombre | '(' expression ')'
 * ```
 *
 * Le moins unaire est traité au niveau `facteur`, ce qui rend `5+−3` analysable — la
 * saisie que la règle 3 du tampon autorise explicitement.
 */
internal class Parser(private val atoms: List<Atom>) {

    private var position = 0

    fun parse(): Expr {
        val expression = parseExpression()
        if (position != atoms.size) fail(EvalError.SYNTAX)
        return expression
    }

    private fun parseExpression(): Expr {
        var left = parseTerm()
        while (true) {
            val operator = peekOperator(Operator.PLUS, Operator.MINUS) ?: return left
            position++
            left = Expr.Binary(left, operator, parseTerm())
        }
    }

    private fun parseTerm(): Expr {
        var left = parseFactor()
        while (true) {
            val operator = peekOperator(Operator.TIMES, Operator.DIVIDE) ?: return left
            position++
            left = Expr.Binary(left, operator, parseFactor())
        }
    }

    private fun parseFactor(): Expr {
        val atom = atoms.getOrNull(position) ?: fail(EvalError.SYNTAX)
        if (atom is Atom.Op && atom.operator == Operator.MINUS) {
            position++
            return Expr.Negate(parseFactor())
        }
        return parsePrimary()
    }

    private fun parsePrimary(): Expr {
        val atom = atoms.getOrNull(position) ?: fail(EvalError.SYNTAX)
        return when (atom) {
            is Atom.Number -> {
                position++
                Expr.Literal(atom.value())
            }

            Atom.LeftParen -> {
                position++
                val inner = parseExpression()
                if (atoms.getOrNull(position) != Atom.RightParen) {
                    fail(EvalError.UNBALANCED_PARENTHESES)
                }
                position++
                inner
            }

            is Atom.Op, Atom.RightParen -> fail(EvalError.SYNTAX)
        }
    }

    /** Renvoie [Nothing] : utilisable comme expression, y compris après un `?:`. */
    private fun fail(error: EvalError): Nothing = throw ParseException(error)

    private fun peekOperator(vararg accepted: Operator): Operator? {
        val atom = atoms.getOrNull(position)
        if (atom !is Atom.Op) return null
        return atom.operator.takeIf { it in accepted }
    }
}
