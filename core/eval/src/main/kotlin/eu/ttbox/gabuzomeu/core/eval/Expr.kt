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

    /**
     * En NPI, un opérateur binaire réclamé alors que moins de deux valeurs sont empilées.
     *
     * Propre à la notation polonaise inverse : en infixe, la même faute est un défaut de
     * syntaxe détecté par le parseur, alors qu'ici il n'y a rien à parser.
     */
    STACK_UNDERFLOW,
}

sealed interface EvalResult {
    data class Success(val value: Rational) : EvalResult
    data class Failure(val error: EvalError) : EvalResult
}
