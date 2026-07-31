package eu.ttbox.gabuzomeu.core.shadok

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * [Rational.parseOrNull] est l'inverse de `toString`.
 *
 * C'est ce couple qui persiste la pile NPI. Le vérifier compte : stocker les valeurs sous
 * forme de fractions plutôt que de décimales est précisément ce qui fait qu'un tiers
 * empilé reste un tiers après un redémarrage du processus.
 */
class RationalTest {

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["0", "7", "-7", "1/3", "-1/3", "22/7", "1/1024"])
    fun `l'aller-retour par le texte est l'identite`(text: String) {
        val value = Rational.parseOrNull(text)

        assertEquals(text, value?.toString(), "relecture de $text")
    }

    @Test
    fun `la fraction relue est reduite et normalisee`() {
        // 2/6 se reduit en 1/3, et le signe remonte au numerateur.
        assertEquals(
            Rational.of(BigInteger.ONE, BigInteger.valueOf(3)),
            Rational.parseOrNull("2/6"),
        )
        assertEquals(Rational.of(-1), Rational.parseOrNull("1/-1"))
    }

    @Test
    fun `un tiers relu est exactement un tiers`() {
        val third = Rational.parseOrNull("1/3")!!

        assertEquals(Rational.ONE, third * Rational.of(3))
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", "  ", "pas-un-nombre", "1/0", "1/2/3", "0.5", "3/x"])
    fun `une entree illisible rend null plutot que de lever`(text: String) {
        assertNull(Rational.parseOrNull(text), "\"$text\" ne devrait pas s'analyser")
    }
}
