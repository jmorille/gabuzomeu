package eu.ttbox.gabuzomeu.core.shadok

import java.math.BigInteger

/**
 * Conversion entre valeurs exactes ([Rational]) et écriture Shadok ([Base4Number]).
 */
object ShadokConverter {

    /**
     * Nombre maximum de chiffres après la virgule produits pour un développement qui
     * ne termine pas. 16 chiffres en base 4 = 32 bits de partie fractionnaire, bien
     * au-delà de ce qu'un écran de calculatrice affiche.
     */
    const val DEFAULT_MAX_FRACTION_DIGITS: Int = 16

    private val RADIX = BigInteger.valueOf(ShadokDigit.RADIX.toLong())
    private val RADIX_RATIONAL = Rational.of(ShadokDigit.RADIX)

    /**
     * Écrit [value] en base 4 Shadok.
     *
     * La partie entière passe par [BigInteger.toString] en base 4. La partie
     * fractionnaire — **absente du code d'origine** — s'obtient par multiplications
     * successives par 4 : à chaque tour, la partie entière du produit donne le chiffre
     * suivant et l'on continue avec le reste.
     *
     * Exemple, 0.5 : 0.5 × 4 = 2.0 → chiffre `2` (Zo), reste 0 → `Ga.Zo`.
     * Le code d'origine produisait `Ga.BuBu`, en convertissant « 5 » comme un entier
     * (5₁₀ = 11₄). Le test d'époque attendait `Ga.Bu`, ce qui est faux aussi : `Ga.Bu`
     * vaut 1×4⁻¹ = 0.25.
     */
    fun toBase4(
        value: Rational,
        maxFractionDigits: Int = DEFAULT_MAX_FRACTION_DIGITS,
    ): Base4Number {
        require(maxFractionDigits >= 0) { "maxFractionDigits négatif : $maxFractionDigits" }

        val magnitude = value.abs()
        val integerPart = magnitude.truncate()

        val integerDigits = integerPart.toString(ShadokDigit.RADIX)
            .map { ShadokDigit.of(it.digitToInt(ShadokDigit.RADIX)) }

        var remainder = magnitude - Rational.of(integerPart)
        val fractionDigits = ArrayList<ShadokDigit>(maxFractionDigits)
        while (!remainder.isZero && fractionDigits.size < maxFractionDigits) {
            remainder *= RADIX_RATIONAL
            val digit = remainder.truncate()
            fractionDigits += ShadokDigit.of(digit.toInt())
            remainder -= Rational.of(digit)
        }

        return Base4Number(
            // signum() == 0 pour zéro, donc pas de « −Ga ».
            negative = value.signum() < 0,
            integerDigits = integerDigits,
            fractionDigits = fractionDigits,
            approximate = !remainder.isZero,
        )
    }

    /**
     * Relit une écriture Shadok en valeur exacte.
     *
     * Pour un [Base4Number] marqué `approximate`, le résultat est la valeur de ce qui
     * a été écrit — donc une approximation de l'original, par construction.
     */
    fun toRational(number: Base4Number): Rational {
        val integer = number.integerDigits
            .fold(BigInteger.ZERO) { acc, digit ->
                acc * RADIX + BigInteger.valueOf(digit.value.toLong())
            }

        // Σ dᵢ·4^-(i+1) = (Σ dᵢ·4^(n-1-i)) / 4ⁿ
        val fractionCount = number.fractionDigits.size
        val result = if (fractionCount == 0) {
            Rational.of(integer)
        } else {
            val scaled = number.fractionDigits
                .fold(BigInteger.ZERO) { acc, digit ->
                    acc * RADIX + BigInteger.valueOf(digit.value.toLong())
                }
            Rational.of(integer) + Rational.of(scaled, RADIX.pow(fractionCount))
        }

        return if (number.negative) -result else result
    }
}
