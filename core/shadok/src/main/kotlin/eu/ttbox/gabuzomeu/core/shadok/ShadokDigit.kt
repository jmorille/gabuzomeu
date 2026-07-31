package eu.ttbox.gabuzomeu.core.shadok

/**
 * Les quatre chiffres Shadok.
 *
 * > Quand il n'y a pas de Shadoks, on dit GA
 * > Quand il y a un shadok de plus, on dit BU
 * > Quand il y a encore un shadok de plus, on dit ZO
 * > Et quand il y a encore un autre, on dit MEU.
 *
 * Ne disposant que de quatre mots, les Shadoks comptent en base 4.
 *
 * Glyphes et libellés sont des **constantes** et non des ressources Android. Le code
 * d'origine les lisait depuis `strings_shadok.xml` via un `Context`, puis les mettait
 * en cache dans des champs `static` derrière un booléen à un coup : ni thread-safe, ni
 * invalidé au changement de locale. Comme `Ga`, `Bu`, `Zo` et `Meu` sont des noms
 * propres issus de la série — ils ne se traduisent pas — les figer ici supprime le
 * problème et rend ce module totalement indépendant d'Android.
 */
enum class ShadokDigit(
    /** Valeur numérique, de 0 à 3. */
    val value: Int,
    /** Symbole affiché. */
    val glyph: Char,
    /** Nom prononcé, utilisé aussi comme description d'accessibilité. */
    val label: String,
) {
    /** 0 — ◯ LARGE CIRCLE (U+25EF). */
    GA(0, '◯', "Ga"),

    /** 1 — _ LOW LINE (U+005F). */
    BU(1, '_', "Bu"),

    /** 2 — ⅃ REVERSED SANS-SERIF CAPITAL L (U+2143). */
    ZO(2, '⅃', "Zo"),

    /** 3 — ◿ LOWER RIGHT TRIANGLE (U+25FF). */
    MEU(3, '◿', "Meu"),
    ;

    /** Le chiffre tel qu'écrit en base 4 : '0', '1', '2' ou '3'. */
    val base4Char: Char get() = '0' + value

    companion object {
        /** La base des Shadoks. */
        const val RADIX: Int = 4

        private val byValue: Map<Int, ShadokDigit> = entries.associateBy { it.value }
        private val byGlyph: Map<Char, ShadokDigit> = entries.associateBy { it.glyph }

        /**
         * @throws IllegalArgumentException si [value] n'est pas dans 0..3. Le code
         * d'origine utilisait un `SparseArray` renvoyant `null`, auto-déballé en `char`
         * — donc un `NullPointerException` opaque dès qu'un caractère inattendu
         * (un signe moins, par exemple) atteignait la table.
         */
        fun of(value: Int): ShadokDigit = requireNotNull(byValue[value]) {
            "Chiffre hors base $RADIX : $value"
        }

        /** `null` si [glyph] n'est pas un glyphe Shadok. */
        fun ofGlyphOrNull(glyph: Char): ShadokDigit? = byGlyph[glyph]

        fun isGlyph(candidate: Char): Boolean = byGlyph.containsKey(candidate)
    }
}
