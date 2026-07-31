package eu.ttbox.gabuzomeu.core.eval

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La table des symboles d'opérateurs.
 *
 * Elle a deux entrées par opération : le vrai caractère mathématique, celui que la
 * calculatrice **affiche**, et sa forme ASCII, celle qu'un clavier physique **tape**. Les
 * confondre était une source d'erreur du code d'origine, qui manipulait les deux sans
 * jamais les distinguer explicitement.
 */
class OperatorTest {

    @ParameterizedTest(name = "{0} s''affiche ''{1}''")
    @CsvSource(
        "PLUS,   +",
        "MINUS,  −",
        "TIMES,  ×",
        "DIVIDE, ÷",
    )
    fun `chaque operateur affiche son vrai caractere mathematique`(
        operator: Operator,
        symbol: Char,
    ) {
        assertEquals(symbol, operator.symbol)
    }

    @Test
    fun `les symboles affiches ne sont pas les formes ASCII`() {
        // U+2212, U+00D7, U+00F7 — et non '-', '*', '/'.
        assertEquals('−', Operator.MINUS.symbol)
        assertEquals('×', Operator.TIMES.symbol)
        assertEquals('÷', Operator.DIVIDE.symbol)
        assertFalse(Operator.entries.any { it.symbol in "-*/" })
    }

    @ParameterizedTest
    @EnumSource(Operator::class)
    fun `ofSymbolOrNull est l'inverse de symbol`(operator: Operator) {
        assertEquals(operator, Operator.ofSymbolOrNull(operator.symbol))
        assertTrue(Operator.isOperator(operator.symbol))
    }

    @ParameterizedTest(name = "''{0}'' -> {1}")
    @CsvSource(
        // Les formes tapées au clavier physique, substituées à la saisie.
        "-, MINUS",
        "*, TIMES",
        "/, DIVIDE",
        // Le plus s'écrit pareil dans les deux mondes.
        "+, PLUS",
    )
    fun `les formes ASCII sont acceptees en entree`(symbol: Char, expected: Operator) {
        assertEquals(expected, Operator.ofSymbolOrNull(symbol))
        assertTrue(Operator.isOperator(symbol))
    }

    @ParameterizedTest(name = "''{0}''")
    @ValueSource(chars = ['0', '9', '(', ')', '.', ' ', 'x', 'X', '=', '◯', '⅃'])
    fun `un caractere qui n'est pas un operateur est refuse`(symbol: Char) {
        // 'x' et 'X' ressemblent à '×', mais ne sont pas des opérateurs : ce sont des
        // caractères qu'un utilisateur peut taper sans vouloir multiplier.
        assertNull(Operator.ofSymbolOrNull(symbol))
        assertFalse(Operator.isOperator(symbol))
    }

    @Test
    fun `isOperator et ofSymbolOrNull sont toujours d'accord`() {
        // Deux façons de poser la même question : elles ne doivent jamais divorcer.
        val candidates = "+-−*×/÷0123456789().= ◯_⅃◿xX"

        candidates.forEach { symbol ->
            assertEquals(
                Operator.ofSymbolOrNull(symbol) != null,
                Operator.isOperator(symbol),
                "désaccord sur '$symbol'",
            )
        }
    }
}
