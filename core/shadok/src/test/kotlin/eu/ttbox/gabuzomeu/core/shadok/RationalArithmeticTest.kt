package eu.ttbox.gabuzomeu.core.shadok

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * L'arithmétique exacte, vérifiée pour elle-même.
 *
 * `RationalTest` couvre l'aller-retour textuel et `ShadokConverterTest` la conversion en
 * base 4 ; les opérations restaient, elles, testées seulement de biais. Or c'est ce type qui
 * porte toute la justesse du projet : le code d'origine déléguait le calcul à la
 * bibliothèque `arity`, qui travaillait en `double`, et une fraction comme 1/3 y devenait
 * `0.333…` arrondi — dont la conversion en base 4 produisait des chiffres de queue faux.
 */
class RationalArithmeticTest {

    private fun r(text: String): Rational = requireNotNull(Rational.parseOrNull(text)) { text }

    // ------------------------------------------------------- construction, normalisation

    @ParameterizedTest(name = "{0}/{1} = {2}")
    @CsvSource(
        "1,  3,  1/3",
        "2,  6,  1/3",
        "50, 100, 1/2",
        "4,  2,  2",
        // Le signe remonte toujours au numérateur, le dénominateur reste positif.
        "-1, 3,  -1/3",
        "1,  -3, -1/3",
        "-1, -3, 1/3",
        // Zéro est canonique : 0/5 et 0/1 sont le même objet.
        "0,  5,  0",
    )
    fun `une fraction est toujours reduite et son signe normalise`(
        numerator: Long,
        denominator: Long,
        expected: String,
    ) {
        val value = Rational.of(BigInteger.valueOf(numerator), BigInteger.valueOf(denominator))

        assertEquals(expected, value.toString())
        assertTrue(value.denominator.signum() > 0, "dénominateur positif")
    }

    @Test
    fun `un denominateur nul est refuse`() {
        val failure = assertThrows<ArithmeticException> {
            Rational.of(BigInteger.ONE, BigInteger.ZERO)
        }

        assertEquals("Dénominateur nul", failure.message)
    }

    @Test
    fun `les constantes valent ce qu'elles disent`() {
        assertEquals(Rational.of(0), Rational.ZERO)
        assertEquals(Rational.of(1), Rational.ONE)
        assertTrue(Rational.ZERO.isZero)
        assertTrue(Rational.ONE.isInteger)
    }

    // ------------------------------------------------------------------- les opérations

    @ParameterizedTest(name = "{0} + {1} = {2}")
    @CsvSource("1/2, 1/3, 5/6", "1/3, -1/3, 0", "2, 3, 5", "1/2, 1/2, 1", "-1/4, 3/4, 1/2")
    fun addition(left: String, right: String, expected: String) {
        assertEquals(r(expected), r(left) + r(right))
    }

    @ParameterizedTest(name = "{0} - {1} = {2}")
    @CsvSource("1/2, 1/3, 1/6", "1/3, 1/3, 0", "3, 5, -2", "-1/2, 1/2, -1")
    fun subtraction(left: String, right: String, expected: String) {
        assertEquals(r(expected), r(left) - r(right))
    }

    @ParameterizedTest(name = "{0} * {1} = {2}")
    @CsvSource("1/2, 2/3, 1/3", "1/3, 3, 1", "-1/2, 1/2, -1/4", "0, 7, 0", "6, 7, 42")
    fun multiplication(left: String, right: String, expected: String) {
        assertEquals(r(expected), r(left) * r(right))
    }

    @ParameterizedTest(name = "{0} / {1} = {2}")
    @CsvSource("1, 3, 1/3", "1/2, 1/4, 2", "-1, 3, -1/3", "1, -3, -1/3", "42, 7, 6")
    fun division(left: String, right: String, expected: String) {
        assertEquals(r(expected), r(left) / r(right))
    }

    @Test
    fun `la division par zero leve, sans corrompre les operandes`() {
        val seven = r("7")

        val failure = assertThrows<ArithmeticException> { seven / Rational.ZERO }

        assertEquals("Division par zéro", failure.message)
        // Immuable : l'échec n'a rien pu abîmer.
        assertEquals(r("7"), seven)
    }

    @Test
    fun `un tiers additionne trois fois vaut exactement un`() {
        // La propriété que le calcul en double ne peut PAS tenir : 0.333…×3 y donne 0.999…
        val third = r("1/3")

        assertEquals(Rational.ONE, third + third + third)
    }

    @Test
    fun `la negation et la valeur absolue`() {
        assertEquals(r("-1/3"), -r("1/3"))
        assertEquals(r("1/3"), -r("-1/3"))
        assertEquals(r("1/3"), r("-1/3").abs())
        assertEquals(r("1/3"), r("1/3").abs())
        // Zéro n'a pas de signe : le nier ne le rend pas négatif.
        assertEquals(Rational.ZERO, -Rational.ZERO)
        assertEquals(0, (-Rational.ZERO).signum())
    }

    @ParameterizedTest(name = "signum({0}) = {1}")
    @CsvSource("0, 0", "1/3, 1", "-1/3, -1", "7, 1", "-7, -1")
    fun signum(text: String, expected: Int) {
        assertEquals(expected, r(text).signum())
    }

    // ---------------------------------------------------- partie entière et fractionnaire

    @ParameterizedTest(name = "truncate({0}) = {1}")
    @CsvSource(
        "7/2,   3",
        "5/2,   2",
        "7,     7",
        "1/3,   0",
        // Troncature VERS ZÉRO, pas vers le bas : -7/2 donne -3 et non -4.
        "-7/2,  -3",
        "-1/3,  0",
    )
    fun `truncate tronque vers zero`(text: String, expected: Long) {
        assertEquals(BigInteger.valueOf(expected), r(text).truncate())
    }

    @ParameterizedTest(name = "fractionalPart({0}) = {1}")
    @CsvSource(
        "7/2,   1/2",
        "7,     0",
        "1/3,   1/3",
        // Signée, donc dans ]-1, 1[ : -7/2 = -3 + (-1/2).
        "-7/2,  -1/2",
        "-1/3,  -1/3",
    )
    fun `fractionalPart est signee et strictement plus petite que un`(
        text: String,
        expected: String,
    ) {
        val value = r(text)

        assertEquals(r(expected), value.fractionalPart())
        // L'invariant qui fait de ShadokConverter.toBase4 une boucle qui termine.
        assertTrue(value.fractionalPart().abs() < Rational.ONE)
        // Et la décomposition se recolle.
        assertEquals(value, Rational.of(value.truncate()) + value.fractionalPart())
    }

    @ParameterizedTest(name = "isInteger({0}) = {1}")
    @CsvSource("7, true", "-7, true", "0, true", "1/3, false", "7/2, false", "4/2, true")
    fun isInteger(text: String, expected: Boolean) {
        assertEquals(expected, r(text).isInteger)
    }

    // ------------------------------------------------------------------------- l'ordre

    @Test
    fun `la comparaison marche a denominateurs differents et signes melanges`() {
        assertTrue(r("1/3") < r("1/2"))
        assertTrue(r("-1/2") < r("-1/3"))
        assertTrue(r("-1/3") < r("1/1000"))
        assertTrue(r("2") > r("5/3"))
        assertEquals(0, r("1/2").compareTo(r("2/4")))
    }

    @Test
    fun `le tri suit l'ordre numerique, pas l'ecriture`() {
        val sorted = listOf(r("1/2"), r("-3"), r("1/3"), r("2"), r("0")).sorted()

        assertEquals(listOf(r("-3"), r("0"), r("1/3"), r("1/2"), r("2")), sorted)
    }

    @Test
    fun `l'egalite est structurelle et coherente avec hashCode`() {
        // Toujours réduit, donc 2/6 et 1/3 sont le même objet — y compris dans un Set.
        assertEquals(r("1/3"), r("2/6"))
        assertEquals(r("1/3").hashCode(), r("2/6").hashCode())
        assertEquals(1, setOf(r("1/3"), r("2/6"), r("3/9")).size)
    }

    // ------------------------------------------------------------- écriture décimale

    @ParameterizedTest(name = "{0} a un decimal fini : {1}")
    @CsvSource(
        // Seuls les facteurs 2 et 5 — ceux de 10 — donnent un développement fini.
        "1/2,    true",
        "1/4,    true",
        "1/5,    true",
        "1/8,    true",
        "1/10,   true",
        "1/20,   true",
        "1/1024, true",
        "3/25,   true",
        "7,      true",
        "1/3,    false",
        "1/6,    false",
        "1/7,    false",
        "2/3,    false",
        "1/15,   false",
    )
    fun hasFiniteDecimal(text: String, expected: Boolean) {
        // Le calcul de cette propriété est aussi l'endroit qui plantait sur Android 12 :
        // ses facteurs premiers étaient écrits `BigInteger.TWO`, absent avant l'API 33, ce
        // qui faisait échouer l'initialisation de la classe — donc TOUTE l'arithmétique.
        // Un test JVM ne peut pas reproduire cela ; il vérifie ici la propriété elle-même.
        assertEquals(expected, r(text).hasFiniteDecimal)
    }

    @ParameterizedTest(name = "{0} -> \"{1}\"")
    @CsvSource(
        "7,       7",
        "-7,      -7",
        "0,       0",
        "1/2,     0.5",
        "-1/8,    -0.125",
        "7/2,     3.5",
        "1/1024,  0.0009765625",
        "3/25,    0.12",
    )
    fun `une valeur a decimal fini se rend exactement`(text: String, expected: String) {
        assertEquals(expected, r(text).toDecimalString())
    }

    @Test
    fun `une valeur sans decimal fini est arrondie, pas rejetee`() {
        // 20 décimales par défaut, arrondi au plus proche : c'est un affichage, pas la valeur.
        assertEquals("0.33333333333333333333", r("1/3").toDecimalString())
        assertEquals("0.66666666666666666667", r("2/3").toDecimalString())
        assertFalse(r("1/3").hasFiniteDecimal, "l'appelant peut savoir que c'est un arrondi")
    }

    @Test
    fun `le nombre de decimales conservees est reglable`() {
        assertEquals("0.333", r("1/3").toDecimalString(maxScale = 3))
        assertEquals("0.7", r("2/3").toDecimalString(maxScale = 1))
    }

    // ---------------------------------------------------------------- depuis un décimal

    @ParameterizedTest(name = "\"{0}\" = {1}")
    @CsvSource(
        "0.5,    1/2",
        "0.25,   1/4",
        "12.34,  617/50",
        "-0.125, -1/8",
        "7,      7",
        "007,    7",
        "0.000,  0",
    )
    fun `ofDecimal ne perd rien, sans passer par un double`(text: String, expected: String) {
        assertEquals(r(expected), Rational.ofDecimal(text))
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["1E+3", "1.5E+2", "1E-3"])
    fun `ofDecimal accepte la notation scientifique, y compris a echelle negative`(text: String) {
        // BigDecimal("1E+3") a une échelle NÉGATIVE (-3) : c'est la branche `else` de
        // ofDecimal, que la seule saisie clavier n'atteint jamais.
        val decimal = BigDecimal(text)

        assertEquals(0, decimal.compareTo(BigDecimal(Rational.ofDecimal(text).toDecimalString())))
    }

    @Test
    fun `les tres grandes valeurs passent par BigInteger sans deborder`() {
        // Aucun Int ni Long dans le chemin : un débordement silencieux est impossible.
        val huge = Rational.of(BigInteger.TEN.pow(40))

        assertEquals(BigInteger.TEN.pow(40), huge.truncate())
        assertEquals(Rational.of(BigInteger.TEN.pow(80)), huge * huge)
        assertEquals(Rational.ONE, huge / huge)
    }

    @Test
    fun `toString est l'ecriture de persistance`() {
        // Le couple toString/parseOrNull est ce qui persiste la pile NPI ; la forme compte.
        assertEquals("7", r("7").toString())
        assertEquals("-1/3", r("-1/3").toString())
        assertEquals("1/3", r("2/6").toString())
    }
}
