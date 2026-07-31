package eu.ttbox.gabuzomeu.core.shadok

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Le socle de tout le reste : les quatre chiffres et leur table de correspondance.
 *
 * Ces tests valent surtout pour les *bords* de la table. C'est là que le code d'origine
 * échouait : son `SparseArray` renvoyait `null` pour un caractère inattendu — un signe
 * moins, typiquement — et l'auto-déballage en `char` transformait ce `null` en
 * `NullPointerException` opaque, loin de la cause.
 */
class ShadokDigitTest {

    @ParameterizedTest(name = "{0} = {1} = ''{2}'' = {3}")
    @CsvSource(
        "GA,  0, ◯, Ga",
        "BU,  1, _, Bu",
        "ZO,  2, ⅃, Zo",
        "MEU, 3, ◿, Meu",
    )
    fun `chaque chiffre porte sa valeur, son glyphe et son nom`(
        digit: ShadokDigit,
        value: Int,
        glyph: Char,
        label: String,
    ) {
        assertEquals(value, digit.value)
        assertEquals(glyph, digit.glyph)
        assertEquals(label, digit.label)
    }

    @Test
    fun `il y a exactement quatre chiffres, et la base vaut quatre`() {
        // « Quand il n'y a pas de Shadoks, on dit GA… » : ne disposant que de quatre mots,
        // les Shadoks comptent en base 4. Le jour où un cinquième chiffre apparaîtrait,
        // RADIX devrait suivre — d'où le lien explicite entre les deux ici.
        assertEquals(ShadokDigit.RADIX, ShadokDigit.entries.size)
        assertEquals(4, ShadokDigit.RADIX)
    }

    @ParameterizedTest
    @EnumSource(ShadokDigit::class)
    fun `of est l'inverse de value`(digit: ShadokDigit) {
        assertEquals(digit, ShadokDigit.of(digit.value))
    }

    @ParameterizedTest
    @EnumSource(ShadokDigit::class)
    fun `ofGlyphOrNull est l'inverse de glyph`(digit: ShadokDigit) {
        assertEquals(digit, ShadokDigit.ofGlyphOrNull(digit.glyph))
        assertTrue(ShadokDigit.isGlyph(digit.glyph))
    }

    @ParameterizedTest(name = "value = {0}")
    @ValueSource(ints = [-1, 4, 5, 10, Int.MAX_VALUE, Int.MIN_VALUE])
    fun `une valeur hors base est refusee explicitement`(value: Int) {
        // Une exception nommée, et non un null qui se transformerait plus loin en NPE.
        val failure = assertThrows<IllegalArgumentException> { ShadokDigit.of(value) }

        assertEquals("Chiffre hors base 4 : $value", failure.message)
    }

    @ParameterizedTest(name = "''{0}''")
    @ValueSource(chars = ['-', '−', '.', '0', '1', 'a', 'O', 'o', 'l', ' '])
    fun `un caractere etranger n'est pas un glyphe`(candidate: Char) {
        // 'O' et 'o' ressemblent à ◯, 'l' à ⅃ : la table ne travaille pas à l'à-peu-près.
        assertNull(ShadokDigit.ofGlyphOrNull(candidate))
        assertFalse(ShadokDigit.isGlyph(candidate))
    }

    @ParameterizedTest
    @EnumSource(ShadokDigit::class)
    fun `base4Char donne le chiffre brut, lisible pour le diagnostic`(digit: ShadokDigit) {
        assertEquals(digit.value, digit.base4Char.digitToInt(ShadokDigit.RADIX))
    }

    @Test
    fun `les quatre glyphes sont distincts`() {
        // Une collision rendrait ofGlyphOrNull ambigu et la table silencieusement fausse.
        assertEquals(ShadokDigit.entries.size, ShadokDigit.entries.map { it.glyph }.toSet().size)
        assertEquals(ShadokDigit.entries.size, ShadokDigit.entries.map { it.label }.toSet().size)
    }
}
