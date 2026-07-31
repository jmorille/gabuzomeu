package eu.ttbox.gabuzomeu.core.shadok

/**
 * Relecture d'un nombre écrit en Shadok — en **glyphes** (`_⅃`) ou en **noms** (`BuZo`).
 *
 * Ne traite qu'un **nombre** ; les expressions complètes (opérateurs, parenthèses)
 * relèvent du module `:core:eval`.
 *
 * Les deux écritures ne diffèrent que par la façon de lire un chiffre : la grammaire —
 * signe, partie entière, séparateur, partie fractionnaire — est écrite une seule fois.
 */
object ShadokParser {

    /**
     * @return le nombre lu, ou `null` si [text] n'est pas un nombre Shadok bien formé.
     *   Un `null` plutôt qu'une exception : une saisie invalide est un cas courant
     *   pendant la frappe, pas une anomalie.
     */
    fun parseGlyphsOrNull(text: String): Base4Number? = parse(text, ::readGlyphAt)

    /**
     * Symétrique de [parseGlyphsOrNull] pour l'écriture en noms : `BuZo`, `−BuZo.Meu`.
     *
     * La casse est libre. C'est délibéré : un nombre recopié depuis un message arrive
     * volontiers en `BUZO`, et le refuser n'apporterait aucune sécurité.
     */
    fun parseLabelsOrNull(text: String): Base4Number? = parse(text, ::readLabelAt)

    /** Un chiffre lu, et l'index où reprendre la lecture. */
    private data class DigitRead(val digit: ShadokDigit, val next: Int)

    private fun parse(text: String, readDigitAt: (String, Int) -> DigitRead?): Base4Number? {
        var index = 0
        val first = text.firstOrNull()
        val negative = first == ShadokFormatter.MINUS || first == '-'
        if (negative) index = 1

        val integerDigits = mutableListOf<ShadokDigit>()
        index = readInto(text, index, integerDigits, readDigitAt)

        val fractionDigits = mutableListOf<ShadokDigit>()
        if (index < text.length && text[index] == ShadokFormatter.SEPARATOR) {
            index = readInto(text, index + 1, fractionDigits, readDigitAt)
        }

        // Il faut au moins un chiffre : « − » seul, « . » seul ou « −. » ne sont pas des
        // nombres. Ce contrôle vient avant le zéro de tête implicite, sinon « . » vaudrait 0.
        if (integerDigits.isEmpty() && fractionDigits.isEmpty()) return null

        // Un reliquat signifie un caractère étranger : la saisie n'est pas un nombre.
        if (index != text.length) return null

        // Un séparateur en tête (« .⅃ ») vaut « ◯.⅃ ».
        if (integerDigits.isEmpty()) integerDigits += ShadokDigit.GA

        return Base4Number(
            negative = negative,
            integerDigits = integerDigits,
            fractionDigits = fractionDigits,
        )
    }

    /** Consomme les chiffres consécutifs à partir de [from] ; renvoie l'index d'arrêt. */
    private fun readInto(
        text: String,
        from: Int,
        into: MutableList<ShadokDigit>,
        readDigitAt: (String, Int) -> DigitRead?,
    ): Int {
        var index = from
        while (index < text.length) {
            val read = readDigitAt(text, index) ?: break
            into += read.digit
            index = read.next
        }
        return index
    }

    private fun readGlyphAt(text: String, index: Int): DigitRead? =
        ShadokDigit.ofGlyphOrNull(text[index])?.let { DigitRead(it, index + 1) }

    private fun readLabelAt(text: String, index: Int): DigitRead? = ShadokDigit.entries
        // Aucun nom n'est le préfixe d'un autre : l'ordre d'essai est sans conséquence.
        .firstOrNull { text.startsWith(it.label, index, ignoreCase = true) }
        ?.let { DigitRead(it, index + it.label.length) }
}
