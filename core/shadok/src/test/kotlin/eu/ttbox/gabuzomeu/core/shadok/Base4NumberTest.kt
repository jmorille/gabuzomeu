package eu.ttbox.gabuzomeu.core.shadok

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Les invariants de l'écriture en base 4.
 *
 * Le point de conception vérifié ici est que le **signe est un champ**, jamais un chiffre.
 * C'est ce qui ferme la classe de bug du code d'origine, où un `-` traversait la table de
 * conversion et y produisait un `NullPointerException`.
 */
class Base4NumberTest {

    @Test
    fun `une partie entiere vide est refusee`() {
        // Zéro s'écrit [GA] et non [] : sans cet invariant, toute boucle sur les chiffres
        // devrait traiter un cas « aucun chiffre » qui n'a pas de sens en écriture positionnelle.
        val failure = assertThrows<IllegalArgumentException> {
            Base4Number(negative = false, integerDigits = emptyList())
        }

        assertEquals("La partie entière ne peut être vide : zéro s'écrit [GA]", failure.message)
    }

    @Test
    fun `une partie fractionnaire vide est le cas normal d'un entier`() {
        val seven = Base4Number(
            negative = false,
            integerDigits = listOf(ShadokDigit.BU, ShadokDigit.MEU),
        )

        assertTrue(seven.fractionDigits.isEmpty())
        assertFalse(seven.approximate)
    }

    @Test
    fun `le signe est un champ, pas un chiffre`() {
        val minusOne = Base4Number(negative = true, integerDigits = listOf(ShadokDigit.BU))

        assertTrue(minusOne.negative)
        // Les chiffres restent la magnitude seule : rien de « signé » ne circule dans la liste.
        assertEquals(listOf(ShadokDigit.BU), minusOne.integerDigits)
    }

    @Test
    fun `zero est zero, et les zeros de tete ne changent rien`() {
        assertTrue(Base4Number(negative = false, integerDigits = listOf(ShadokDigit.GA)).isZero)
        assertTrue(
            Base4Number(
                negative = false,
                integerDigits = listOf(ShadokDigit.GA, ShadokDigit.GA, ShadokDigit.GA),
            ).isZero,
        )
    }

    @Test
    fun `tout chiffre non nul suffit a ne plus etre zero`() {
        assertFalse(Base4Number(negative = false, integerDigits = listOf(ShadokDigit.BU)).isZero)
        assertFalse(
            Base4Number(
                negative = false,
                integerDigits = listOf(ShadokDigit.GA, ShadokDigit.BU),
            ).isZero,
        )
    }

    @Test
    fun `une partie fractionnaire presente ecarte le cas zero`() {
        // `isZero` est un test sur l'ÉCRITURE, pas sur la valeur : « Ga.Ga » vaut bien zéro
        // mais n'est pas l'écriture canonique de zéro. La distinction est sans danger parce
        // que ShadokConverter n'émet jamais de zéro de queue — sa boucle s'arrête dès que le
        // reste est nul, donc « Ga.Ga » n'est pas produit. Le test fixe cette frontière pour
        // qu'un futur appelant ne prenne pas isZero pour une comparaison numérique.
        val writtenZeroWithFraction = Base4Number(
            negative = false,
            integerDigits = listOf(ShadokDigit.GA),
            fractionDigits = listOf(ShadokDigit.GA),
        )

        assertFalse(writtenZeroWithFraction.isZero)
        assertTrue(ShadokConverter.toRational(writtenZeroWithFraction).isZero)
    }

    @Test
    fun `le converter n'emet jamais de zero de queue`() {
        // L'invariant sur lequel repose le test précédent.
        val values = listOf(
            Rational.ZERO,
            Rational.ofDecimal("0.5"),
            Rational.ofDecimal("0.25"),
            Rational.ofDecimal("6.25"),
            Rational.of(-3),
            // Un développement tronqué : le dernier chiffre écrit n'est pas un Ga non plus.
            Rational.parseOrNull("1/3")!!,
        )

        values.forEach { value ->
            val digits = ShadokConverter.toBase4(value).fractionDigits
            assertTrue(
                digits.isEmpty() || digits.last() != ShadokDigit.GA,
                "$value ne devrait pas finir par un Ga fractionnaire : $digits",
            )
        }
    }

    @Test
    fun `deux ecritures identiques sont egales`() {
        // data class : l'égalité structurelle est ce qui rend les assertions de test lisibles.
        val first = Base4Number(
            negative = true,
            integerDigits = listOf(ShadokDigit.BU),
            fractionDigits = listOf(ShadokDigit.ZO),
            approximate = true,
        )
        val second = Base4Number(
            negative = true,
            integerDigits = listOf(ShadokDigit.BU),
            fractionDigits = listOf(ShadokDigit.ZO),
            approximate = true,
        )

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
    }
}
