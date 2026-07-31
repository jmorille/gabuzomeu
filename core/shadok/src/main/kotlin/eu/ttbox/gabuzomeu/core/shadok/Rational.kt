package eu.ttbox.gabuzomeu.core.shadok

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

/**
 * Fraction exacte, toujours réduite, dénominateur strictement positif.
 *
 * C'est le type numérique interne du projet, et ce choix est ce qui rend la conversion
 * en base 4 correcte. Comme 4 = 2², le développement d'une fraction irréductible `p/q`
 * termine **si et seulement si `q` est une puissance de deux**. Avec un [BigDecimal],
 * `1/3` devient `0.333…` arrondi, dont la conversion produit des chiffres de queue
 * faux ; avec un [Rational], `1/3` reste `1/3` et se rend `Ga.BuBuBu…` — le vrai
 * développement périodique — tout en sachant dire qu'il est tronqué.
 *
 * Le code d'origine déléguait le calcul à la bibliothèque arity, qui travaillait en
 * `double`.
 */
class Rational private constructor(val numerator: BigInteger, val denominator: BigInteger) :
    Comparable<Rational> {

    init {
        require(denominator.signum() > 0) { "Dénominateur non normalisé : $denominator" }
    }

    val isZero: Boolean get() = numerator.signum() == 0
    val isInteger: Boolean get() = denominator == BigInteger.ONE

    fun signum(): Int = numerator.signum()

    fun abs(): Rational = if (signum() >= 0) this else Rational(numerator.negate(), denominator)

    operator fun unaryMinus(): Rational = Rational(numerator.negate(), denominator)

    operator fun plus(other: Rational): Rational = of(
        numerator * other.denominator + other.numerator * denominator,
        denominator * other.denominator,
    )

    operator fun minus(other: Rational): Rational = this + (-other)

    operator fun times(other: Rational): Rational =
        of(numerator * other.numerator, denominator * other.denominator)

    /** @throws ArithmeticException si [other] est nul. */
    operator fun div(other: Rational): Rational {
        if (other.isZero) throw ArithmeticException("Division par zéro")
        return of(numerator * other.denominator, denominator * other.numerator)
    }

    /** Partie entière, tronquée **vers zéro** (BigInteger.divide tronque déjà ainsi). */
    fun truncate(): BigInteger = numerator / denominator

    /** Partie fractionnaire signée : `this - truncate()`, donc dans `]-1, 1[`. */
    fun fractionalPart(): Rational = this - of(truncate())

    /**
     * `true` si l'écriture décimale de cette valeur est finie.
     *
     * C'est le cas si et seulement si le dénominateur réduit ne contient que des
     * facteurs 2 et 5 — les facteurs premiers de 10. Permet de savoir si
     * [toDecimalString] rend la valeur exactement ou l'arrondit.
     */
    val hasFiniteDecimal: Boolean
        get() {
            var remaining = denominator
            for (factor in DECIMAL_PRIME_FACTORS) {
                while (remaining.mod(factor).signum() == 0) {
                    remaining /= factor
                }
            }
            return remaining == BigInteger.ONE
        }

    /**
     * Écriture décimale, exacte quand elle est possible.
     *
     * Tout nombre à développement fini en base 4 en a aussi un en base 10 (4 = 2² et 2
     * divise 10), donc une valeur issue d'une saisie Shadok se rend toujours
     * exactement. Sinon — un tiers, par exemple — on arrondit à [maxScale] décimales.
     */
    fun toDecimalString(maxScale: Int = DEFAULT_MAX_SCALE): String {
        if (isInteger) return numerator.toString()
        val exact = runCatching { BigDecimal(numerator).divide(BigDecimal(denominator)) }
            .getOrNull()
        val decimal = exact
            ?: BigDecimal(numerator).divide(BigDecimal(denominator), maxScale, RoundingMode.HALF_UP)
        return decimal.stripTrailingZeros().toPlainString()
    }

    override fun compareTo(other: Rational): Int =
        // Les deux dénominateurs sont positifs : le produit croisé préserve l'ordre.
        (numerator * other.denominator).compareTo(other.numerator * denominator)

    override fun equals(other: Any?): Boolean = when {
        this === other -> true

        other !is Rational -> false

        // Toujours réduit, donc l'égalité composante par composante suffit.
        else -> numerator == other.numerator && denominator == other.denominator
    }

    override fun hashCode(): Int = 31 * numerator.hashCode() + denominator.hashCode()

    override fun toString(): String =
        if (isInteger) numerator.toString() else "$numerator/$denominator"

    companion object {
        /** Décimales conservées quand le développement décimal ne termine pas. */
        const val DEFAULT_MAX_SCALE: Int = 20

        /**
         * Facteurs premiers de 10, ceux qui rendent un développement décimal fini.
         *
         * `BigInteger.valueOf(2)` et non `BigInteger.TWO` : cette constante n'existe sur
         * Android qu'à partir de l'API 33, alors que `minSdk` est 31. Elle faisait donc
         * échouer l'initialisation de cette classe — donc **toute** arithmétique de
         * l'application — d'un `NoSuchFieldError` sur Android 12 et 12L. Rien ne l'avait
         * signalé : ce module est du Kotlin pur, hors de portée du `NewApi` d'Android Lint,
         * et l'appareil de développement tourne en API 37.
         */
        private val DECIMAL_PRIME_FACTORS =
            listOf(BigInteger.valueOf(2), BigInteger.valueOf(5))

        val ZERO: Rational = Rational(BigInteger.ZERO, BigInteger.ONE)
        val ONE: Rational = Rational(BigInteger.ONE, BigInteger.ONE)

        fun of(value: Int): Rational = of(BigInteger.valueOf(value.toLong()))

        fun of(value: BigInteger): Rational = Rational(value, BigInteger.ONE)

        fun of(numerator: BigInteger, denominator: BigInteger): Rational {
            if (denominator.signum() == 0) throw ArithmeticException("Dénominateur nul")
            // Le signe est porté par le numérateur, le dénominateur reste positif.
            val sign = denominator.signum()
            val n = if (sign < 0) numerator.negate() else numerator
            val d = if (sign < 0) denominator.negate() else denominator
            if (n.signum() == 0) return ZERO
            val gcd = n.gcd(d)
            return Rational(n / gcd, d / gcd)
        }

        /**
         * Convertit un décimal exact (`"12.34"`) en fraction exacte (`1234/100`).
         * Aucune perte : on passe par les entrailles de [BigDecimal] plutôt que par un
         * `double`.
         */
        fun ofDecimal(text: String): Rational = ofDecimal(BigDecimal(text))

        /**
         * Relit ce que produit [toString] : `"7"` ou `"-1/3"`.
         *
         * C'est l'écriture de persistance de la pile NPI. Passer par la fraction plutôt
         * que par le décimal est ce qui la rend **exacte** : un tiers empilé se retrouve
         * après redémarrage comme un tiers, et non comme `0.33333333333333333333`.
         */
        fun parseOrNull(text: String): Rational? {
            val parts = text.trim().split('/')
            if (parts.size > 2) return null
            return runCatching {
                val numerator = BigInteger(parts[0])
                if (parts.size == 1) of(numerator) else of(numerator, BigInteger(parts[1]))
            }.getOrNull()
        }

        fun ofDecimal(value: BigDecimal): Rational {
            val unscaled = value.unscaledValue()
            val scale = value.scale()
            return if (scale >= 0) {
                of(unscaled, BigInteger.TEN.pow(scale))
            } else {
                of(unscaled * BigInteger.TEN.pow(-scale), BigInteger.ONE)
            }
        }
    }
}
