package eu.ttbox.gabuzomeu.widget

import androidx.annotation.DrawableRes
import eu.ttbox.gabuzomeu.core.shadok.Rational
import eu.ttbox.gabuzomeu.core.shadok.ShadokClock
import eu.ttbox.gabuzomeu.core.shadok.ShadokConverter
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation

/**
 * Un élément à dessiner : un chiffre Shadok, ou un séparateur.
 *
 * Le séparateur **porte son caractère** plutôt que d'être un singleton : l'heure sépare ses
 * champs par un deux-points, la date par une barre oblique. Sans cela, la date se serait
 * affichée avec le séparateur de l'heure.
 */
internal sealed interface ShadokTimeSymbol {
    data class Digit(val digit: ShadokDigit) : ShadokTimeSymbol
    data class Separator(val character: Char) : ShadokTimeSymbol
}

/**
 * L'heure Shadok découpée en symboles dessinables, et la ressource de chaque chiffre.
 *
 * Glance rend du `RemoteViews` : il ne sait afficher ni un `ImageVector` Compose, ni une
 * police embarquée. Un glyphe est donc un `Image` pointant une ressource vectorielle, et
 * l'heure une suite de tels `Image` — d'où ce découpage.
 *
 * On passe par [ShadokClock.format] plutôt que de reconvertir les heures et les minutes
 * ici : la convention de l'heure Shadok — chaque composante convertie **comme un nombre**,
 * et non chiffre à chiffre, si bien que 14:35 donne `MeuZo:ZoGaMeu` — reste définie à un
 * seul endroit. Ce module ne fait que la relire.
 */
internal object ShadokTimeGlyphs {

    /**
     * @throws IllegalArgumentException si [hour] ou [minute] sort de sa plage — la même
     *   garde que [ShadokClock.format], qu'on ne fait que propager.
     */
    fun symbolsOf(hour: Int, minute: Int): List<ShadokTimeSymbol> =
        ShadokClock.format(hour, minute, ShadokNotation.GLYPHS).map { character ->
            val digit = ShadokDigit.ofGlyphOrNull(character)
            if (digit != null) {
                ShadokTimeSymbol.Digit(digit)
            } else {
                // Le seul non-chiffre attendu. Un `else` silencieux masquerait une
                // évolution du format sous un affichage muet.
                require(character == ShadokClock.TIME_SEPARATOR) {
                    "Caractère inattendu dans l'heure Shadok : $character"
                }
                ShadokTimeSymbol.Separator(character)
            }
        }

    /** Le séparateur de la date : une barre oblique, pas le deux-points de l'heure. */
    const val DATE_SEPARATOR: Char = '/'

    /**
     * La date en chiffres Shadok : jour, mois et **année**, chacun converti comme un nombre.
     *
     * Même convention que [ShadokClock] pour l'heure : c'est la valeur du champ qui passe en
     * base 4, et non ses chiffres décimaux un à un. Le 31 juillet 2026 donne donc
     * `133/13/133222` — onze chiffres, dont six pour la seule année, ce qui explique que la
     * date se dessine en plus petit que l'heure.
     */
    fun dateSymbolsOf(day: Int, month: Int, year: Int): List<ShadokTimeSymbol> = buildList {
        listOf(day, month, year).forEachIndexed { index, field ->
            if (index > 0) add(ShadokTimeSymbol.Separator(DATE_SEPARATOR))
            digitsOf(field).forEach { digit -> add(ShadokTimeSymbol.Digit(digit)) }
        }
    }

    /** La date écrite en noms prononcés ou en chiffres bruts. */
    fun formatDate(day: Int, month: Int, year: Int, notation: ShadokNotation): String =
        listOf(day, month, year).joinToString(DATE_SEPARATOR.toString()) { field ->
            digitsOf(field).joinToString("") { digit -> render(digit, notation) }
        }

    private fun render(digit: ShadokDigit, notation: ShadokNotation): String = when (notation) {
        ShadokNotation.GLYPHS -> digit.glyph.toString()
        ShadokNotation.LABELS -> digit.label
        ShadokNotation.BASE4 -> digit.base4Char.toString()
    }

    private fun digitsOf(value: Int): List<ShadokDigit> =
        ShadokConverter.toBase4(Rational.of(value)).integerDigits

    /**
     * Ce que doit lire TalkBack : les **noms**, jamais les formes.
     *
     * Les dessins n'ont pas de texte alternatif par eux-mêmes ; sans cela, l'horloge serait
     * muette pour un lecteur d'écran.
     */
    fun labelsOf(hour: Int, minute: Int): String =
        ShadokClock.format(hour, minute, ShadokNotation.LABELS)

    /**
     * Un exemple d'heure dans une notation, pour l'écran de configuration.
     *
     * 6 h 6 est choisi à dessein : 6 vaut 12 en base 4, donc l'aperçu comporte deux chiffres
     * différents dans chaque écriture — `_⅃`, `BuZo` ou `12`. Une heure comme 1 h 1 n'aurait
     * rien montré de la façon dont les chiffres s'enchaînent.
     */
    fun previewOf(notation: ShadokNotation): String =
        ShadokClock.format(PREVIEW_HOUR, PREVIEW_MINUTE, notation)

    private const val PREVIEW_HOUR = 6
    private const val PREVIEW_MINUTE = 6

    @DrawableRes
    fun drawableOf(digit: ShadokDigit): Int = when (digit) {
        ShadokDigit.GA -> R.drawable.shadok_ga
        ShadokDigit.BU -> R.drawable.shadok_bu
        ShadokDigit.ZO -> R.drawable.shadok_zo
        ShadokDigit.MEU -> R.drawable.shadok_meu
    }
}
