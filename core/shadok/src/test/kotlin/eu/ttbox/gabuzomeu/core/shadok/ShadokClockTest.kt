package eu.ttbox.gabuzomeu.core.shadok

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertEquals

class ShadokClockTest {

    @ParameterizedTest(name = "{0}h{1} = {2}")
    @CsvSource(
        // L'exemple documente dans docs/shadok-reference.md :
        // 14 = 32 en base 4 -> MeuZo ; 35 = 203 en base 4 -> ZoGaMeu
        "14, 35, MeuZo:ZoGaMeu",
        "0,  0,  Ga:Ga",
        "1,  1,  Bu:Bu",
        "3,  3,  Meu:Meu",
        "4,  4,  BuGa:BuGa",
        "12, 30, MeuGa:BuMeuZo",
        "23, 59, BuBuMeu:MeuZoMeu",
    )
    fun formatsTimeAsShadokNames(hour: Int, minute: Int, expected: String) {
        assertEquals(expected, ShadokClock.format(hour, minute))
    }

    @Test
    fun `les glyphes sont aussi disponibles`() {
        // 6 = 12 en base 4 -> _⅃
        assertEquals("_⅃:_⅃", ShadokClock.format(6, 6, ShadokNotation.GLYPHS))
    }

    @Test
    fun `une heure hors plage est refusee`() {
        assertThrows<IllegalArgumentException> { ShadokClock.format(24, 0) }
        assertThrows<IllegalArgumentException> { ShadokClock.format(-1, 0) }
        assertThrows<IllegalArgumentException> { ShadokClock.format(0, 60) }
    }
}
