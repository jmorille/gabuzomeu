package eu.ttbox.gabuzomeu.core.eval

import eu.ttbox.gabuzomeu.core.shadok.Rational
import org.junit.jupiter.api.Test
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** La pile NPI seule, sans la frappe : empiler, dépiler, échanger, réduire. */
class RpnStackTest {

    private fun stackOf(vararg values: Int) = RpnStack(values.map(Rational::of))

    @Test
    fun `le sommet est la derniere valeur empilee`() {
        val stack = RpnStack().push(Rational.of(2)).push(Rational.of(7))

        assertEquals(2, stack.depth)
        assertEquals(Rational.of(7), stack.top)
    }

    @Test
    fun `depiler une pile vide n'est pas une erreur`() {
        assertTrue(RpnStack().dropTop().isEmpty)
    }

    @Test
    fun `echanger reclame deux valeurs`() {
        assertNull(RpnStack().swapTop())
        assertNull(stackOf(5).swapTop())
        assertEquals(stackOf(7, 2), stackOf(2, 7).swapTop())
    }

    @Test
    fun `changer de signe n'agit que sur le sommet`() {
        assertEquals(stackOf(2, -7), stackOf(2, 7).negateTop())
        // Une pile vide reste vide plutot que de lever.
        assertTrue(RpnStack().negateTop().isEmpty)
    }

    @Test
    fun `le sommet est l'operande droit`() {
        // 10 ENTER 3 − vaut 7, pas -7 : l'ordre de frappe est preserve.
        assertEquals(stackOf(7), stackOf(10, 3).apply(Operator.MINUS))
        assertEquals(stackOf(4), stackOf(12, 3).apply(Operator.DIVIDE))
    }

    @Test
    fun `l'operateur consomme deux valeurs et n'en rend qu'une`() {
        val reduced = stackOf(1, 2, 3).apply(Operator.PLUS)

        assertEquals(stackOf(1, 5), reduced)
        // Le fond de pile est intact : rien n'est perdu au-dela des deux operandes.
        assertEquals(Rational.of(1), reduced?.values?.first())
    }

    @Test
    fun `un operateur sur une pile trop courte est refuse`() {
        assertNull(RpnStack().apply(Operator.PLUS))
        assertNull(stackOf(5).apply(Operator.PLUS))
    }

    @Test
    fun `la division par zero leve et laisse la pile intacte`() {
        val stack = stackOf(5, 0)

        val thrown = runCatching { stack.apply(Operator.DIVIDE) }.exceptionOrNull()

        assertTrue(thrown is ArithmeticException, "obtenu $thrown")
        // La pile est immuable : les deux operandes sont forcement toujours la.
        assertEquals(stackOf(5, 0), stack)
    }

    @Test
    fun `l'arithmetique reste exacte`() {
        val third = stackOf(1, 3).apply(Operator.DIVIDE)?.top

        assertEquals(Rational.of(BigInteger.ONE, BigInteger.valueOf(3)), third)
    }

    // ------------------------------------------------------------- persistance

    @Test
    fun `l'aller-retour de persistance conserve les fractions exactement`() {
        val stack = stackOf(1, 3).apply(Operator.DIVIDE)!!.push(Rational.of(-7))

        val restored = RpnStack.restore(stack.keys())

        assertEquals(stack, restored)
        // Ce sont bien les fractions qui sont stockees, non leur arrondi decimal.
        assertEquals("1/3;-7", stack.keys())
    }

    @Test
    fun `une pile vide se persiste et se relit`() {
        assertEquals("", RpnStack().keys())
        assertTrue(RpnStack.restore("").isEmpty)
    }

    @Test
    fun `les fragments illisibles sont ignores plutot que fatals`() {
        // Donnees corrompues sur disque : on garde ce qui est lisible.
        assertEquals(stackOf(2, 5), RpnStack.restore("2;pas-un-nombre;5;1/0"))
    }
}
