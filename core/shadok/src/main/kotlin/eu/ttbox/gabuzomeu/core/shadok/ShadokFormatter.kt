package eu.ttbox.gabuzomeu.core.shadok

/** Les trois écritures possibles d'un [Base4Number]. */
enum class ShadokNotation {
    /** Glyphes : `_⅃` pour 6. */
    GLYPHS,

    /** Noms prononcés : `BuZo` pour 6. Aussi la forme lue par TalkBack. */
    LABELS,

    /** Chiffres bruts en base 4 : `12` pour 6. Utile au diagnostic et aux tests. */
    BASE4,
}

/**
 * Rend un [Base4Number] sous l'une des trois notations.
 *
 * Les symboles suivent la convention d'affichage de la calculatrice d'origine : le
 * moins est le vrai signe mathématique U+2212, pas le trait d'union ASCII.
 */
object ShadokFormatter {

    /** − U+2212 MINUS SIGN. */
    const val MINUS: Char = '−'

    /** Séparateur décimal. */
    const val SEPARATOR: Char = '.'

    /** ≈ U+2248 ALMOST EQUAL TO, préfixe des développements tronqués. */
    const val APPROXIMATION: Char = '≈'

    /**
     * @param markApproximation préfixe le résultat de [APPROXIMATION] quand le
     *   développement en base 4 ne termine pas (1/3, 0.1 décimal…). Le désactiver sert
     *   aux tests, qui comparent les chiffres eux-mêmes.
     */
    fun format(
        number: Base4Number,
        notation: ShadokNotation,
        markApproximation: Boolean = true,
    ): String = buildString {
        if (markApproximation && number.approximate) append(APPROXIMATION)
        if (number.negative) append(MINUS)
        number.integerDigits.forEach { append(it.render(notation)) }
        if (number.fractionDigits.isNotEmpty()) {
            append(SEPARATOR)
            number.fractionDigits.forEach { append(it.render(notation)) }
        }
    }

    private fun ShadokDigit.render(notation: ShadokNotation): String = when (notation) {
        ShadokNotation.GLYPHS -> glyph.toString()
        ShadokNotation.LABELS -> label
        ShadokNotation.BASE4 -> base4Char.toString()
    }
}
