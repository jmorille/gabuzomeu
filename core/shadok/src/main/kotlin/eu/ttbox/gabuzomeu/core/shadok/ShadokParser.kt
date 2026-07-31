package eu.ttbox.gabuzomeu.core.shadok

/**
 * Relecture d'un nombre écrit en glyphes Shadok.
 *
 * Ne traite qu'un **nombre** ; les expressions complètes (opérateurs, parenthèses)
 * relèvent du module `:core:eval`.
 */
object ShadokParser {

    /**
     * @return le nombre lu, ou `null` si [text] n'est pas un nombre Shadok bien formé.
     *   Un `null` plutôt qu'une exception : une saisie invalide est un cas courant
     *   pendant la frappe, pas une anomalie.
     */
    fun parseGlyphsOrNull(text: String): Base4Number? {
        // Il faut au moins un glyphe : « − » seul ou « . » seul n'est pas un nombre.
        if (text.none { ShadokDigit.isGlyph(it) }) return null

        var index = 0
        val negative = text[0] == ShadokFormatter.MINUS || text[0] == '-'
        if (negative) index = 1

        val integerDigits = mutableListOf<ShadokDigit>()
        index = readDigitsInto(text, index, integerDigits)
        // Un séparateur en tête (« .⅃ ») vaut « ◯.⅃ ».
        if (integerDigits.isEmpty()) integerDigits += ShadokDigit.GA

        val fractionDigits = mutableListOf<ShadokDigit>()
        if (index < text.length && text[index] == ShadokFormatter.SEPARATOR) {
            index = readDigitsInto(text, index + 1, fractionDigits)
        }

        // Un reliquat signifie un caractère étranger : la saisie n'est pas un nombre.
        if (index != text.length) return null

        return Base4Number(
            negative = negative,
            integerDigits = integerDigits,
            fractionDigits = fractionDigits,
        )
    }

    /** Consomme les glyphes consécutifs à partir de [from] ; renvoie l'index d'arrêt. */
    private fun readDigitsInto(text: String, from: Int, into: MutableList<ShadokDigit>): Int {
        var index = from
        while (index < text.length) {
            val digit = ShadokDigit.ofGlyphOrNull(text[index]) ?: break
            into += digit
            index++
        }
        return index
    }
}
