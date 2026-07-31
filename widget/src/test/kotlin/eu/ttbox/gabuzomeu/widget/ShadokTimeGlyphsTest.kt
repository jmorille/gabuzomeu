package eu.ttbox.gabuzomeu.widget

import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Le découpage de l'heure en symboles dessinables.
 *
 * C'est ce qui permet au widget d'afficher enfin des **glyphes** et non les noms prononcés.
 * Testable sur la JVM parce que le découpage ne connaît ni Glance ni Android : il ne rend
 * qu'une liste, que la couche d'affichage traduit ensuite en `Image`.
 */
class ShadokTimeGlyphsTest {

    private fun digits(hour: Int, minute: Int): List<ShadokDigit> =
        ShadokTimeGlyphs.symbolsOf(hour, minute)
            .filterIsInstance<ShadokTimeSymbol.Digit>()
            .map { it.digit }

    @ParameterizedTest(name = "{0}h{1} = {2}")
    @CsvSource(
        // La convention de ShadokClock : chaque composante convertie COMME UN NOMBRE.
        // 14 = 32 en base 4 -> MeuZo ; 35 = 203 -> ZoGaMeu.
        "14, 35, MEU|ZO|:|ZO|GA|MEU",
        "0,  0,  GA|:|GA",
        "1,  1,  BU|:|BU",
        "6,  6,  BU|ZO|:|BU|ZO",
        // Le pire cas de largeur : trois chiffres de chaque côté.
        "23, 59, BU|BU|MEU|:|MEU|ZO|MEU",
    )
    fun `l'heure se decoupe en chiffres et en separateur`(
        hour: Int,
        minute: Int,
        expected: String,
    ) {
        val rendered = ShadokTimeGlyphs.symbolsOf(hour, minute).joinToString("|") { symbol ->
            when (symbol) {
                is ShadokTimeSymbol.Digit -> symbol.digit.name
                is ShadokTimeSymbol.Separator -> symbol.character.toString()
            }
        }

        assertEquals(expected, rendered)
    }

    @Test
    fun `il y a toujours exactement un separateur`() {
        // Une heure sans deux-points, ou avec deux, serait illisible. La propriété tient pour
        // les 1440 minutes de la journée, pas seulement pour les exemples ci-dessus.
        for (hour in 0..23) {
            for (minute in 0..59) {
                val separators = ShadokTimeGlyphs.symbolsOf(hour, minute)
                    .count { it is ShadokTimeSymbol.Separator }

                assertEquals(1, separators, "$hour:$minute")
            }
        }
    }

    @Test
    fun `chaque chiffre de la journee est un vrai chiffre Shadok`() {
        // Autrement dit : aucun symbole ne peut arriver à l'affichage sans dessin associé.
        for (hour in 0..23) {
            for (minute in 0..59) {
                digits(hour, minute).forEach { digit ->
                    assertTrue(digit in ShadokDigit.entries, "$hour:$minute -> $digit")
                }
            }
        }
    }

    @Test
    fun `le pire cas tient en six chiffres`() {
        // La borne sur laquelle repose le choix de GLYPH_SIZE dans ShadokClockWidget : si
        // elle changeait, la largeur du widget ne suffirait plus.
        val widest = (0..23).flatMap { hour ->
            (0..59).map { minute -> digits(hour, minute).size }
        }.max()

        assertEquals(6, widest)
    }

    @ParameterizedTest(name = "{0}h{1} se lit \"{2}\"")
    @CsvSource(
        "14, 35, MeuZo:ZoGaMeu",
        "0,  0,  Ga:Ga",
        "23, 59, BuBuMeu:MeuZoMeu",
    )
    fun `l'accessibilite annonce les noms, pas les formes`(
        hour: Int,
        minute: Int,
        expected: String,
    ) {
        // Les dessins sont muets : sans cette description, l'horloge serait invisible pour
        // un lecteur d'écran.
        assertEquals(expected, ShadokTimeGlyphs.labelsOf(hour, minute))
    }

    // ------------------------------------------------------------ la date en Shadok

    @ParameterizedTest(name = "{0}/{1}/{2} = {3}")
    @CsvSource(
        // Chaque champ converti COMME UN NOMBRE, pas chiffre à chiffre :
        // 31 = 133, juillet = 7 = 13, 2026 = 133222.
        "31, 7,  2026, 133/13/133222",
        "1,  1,  2026, 1/1/133222",
        // 2000 = 133100 (1024+768+192+16) et 2024 = 133220 (1024+768+192+32+8).
        "4,  12, 2000, 10/30/133100",
        "29, 2,  2024, 131/2/133220",
    )
    fun `la date se decoupe champ par champ en base 4`(
        day: Int,
        month: Int,
        year: Int,
        expected: String,
    ) {
        assertEquals(
            expected,
            ShadokTimeGlyphs.formatDate(day, month, year, ShadokNotation.BASE4),
        )
    }

    @Test
    fun `la date en noms est lisible, et en glyphes dessinable`() {
        // 31 juillet 2026. Les noms disent la même chose que les chiffres.
        assertEquals(
            "BuMeuMeu/BuMeu/BuMeuMeuZoZoZo",
            ShadokTimeGlyphs.formatDate(31, 7, 2026, ShadokNotation.LABELS),
        )

        val symbols = ShadokTimeGlyphs.dateSymbolsOf(31, 7, 2026)
        // Onze chiffres — dont six pour la seule année — et deux séparateurs. C'est cette
        // longueur qui justifie des glyphes de date plus petits que ceux de l'heure.
        assertEquals(11, symbols.count { it is ShadokTimeSymbol.Digit })
        assertEquals(2, symbols.count { it is ShadokTimeSymbol.Separator })
    }

    @Test
    fun `le separateur de la date n'est pas celui de l'heure`() {
        // Sans caractère porté par le symbole, la date se serait affichée avec un deux-points.
        val dateSeparators = ShadokTimeGlyphs.dateSymbolsOf(31, 7, 2026)
            .filterIsInstance<ShadokTimeSymbol.Separator>()
            .map { it.character }
        val timeSeparators = ShadokTimeGlyphs.symbolsOf(22, 22)
            .filterIsInstance<ShadokTimeSymbol.Separator>()
            .map { it.character }

        assertEquals(listOf('/', '/'), dateSeparators)
        assertEquals(listOf(':'), timeSeparators)
    }

    @Test
    fun `la date en glyphes rend les memes chiffres que les noms`() {
        // Les deux écritures doivent décrire la même suite de chiffres, sinon l'utilisateur
        // verrait une date différente selon le réglage choisi.
        val fromSymbols = ShadokTimeGlyphs.dateSymbolsOf(31, 7, 2026).joinToString("") { symbol ->
            when (symbol) {
                is ShadokTimeSymbol.Digit -> symbol.digit.base4Char.toString()
                is ShadokTimeSymbol.Separator -> symbol.character.toString()
            }
        }

        assertEquals(
            ShadokTimeGlyphs.formatDate(31, 7, 2026, ShadokNotation.BASE4),
            fromSymbols,
        )
    }

    @Test
    fun `une heure hors plage est refusee, comme dans ShadokClock`() {
        assertThrows<IllegalArgumentException> { ShadokTimeGlyphs.symbolsOf(24, 0) }
        assertThrows<IllegalArgumentException> { ShadokTimeGlyphs.symbolsOf(-1, 0) }
        assertThrows<IllegalArgumentException> { ShadokTimeGlyphs.symbolsOf(0, 60) }
    }
}
