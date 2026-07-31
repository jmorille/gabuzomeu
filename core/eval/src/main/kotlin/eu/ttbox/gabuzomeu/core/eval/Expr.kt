package eu.ttbox.gabuzomeu.core.eval

import eu.ttbox.gabuzomeu.core.shadok.Rational

/** Arbre syntaxique d'une expression arithmétique. */
sealed interface Expr {
    data class Literal(val value: Rational) : Expr
    data class Negate(val operand: Expr) : Expr
    data class Binary(val left: Expr, val operator: Operator, val right: Expr) : Expr
}

/** Ce qui peut empêcher une expression d'être évaluée. */
enum class EvalError {
    /** Rien à évaluer. */
    EMPTY,

    /** Expression mal formée. */
    SYNTAX,

    /** Parenthèse fermante sans ouvrante correspondante. */
    UNBALANCED_PARENTHESES,

    DIVISION_BY_ZERO,
}

sealed interface EvalResult {
    data class Success(val value: Rational) : EvalResult
    data class Failure(val error: EvalError) : EvalResult
}
