package eu.ttbox.gabuzomeu.core.eval

import eu.ttbox.gabuzomeu.core.shadok.Rational
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.core.shadok.ShadokNotation
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * La conversion « chiffres tels que saisis » → valeur exacte.
 *
 * Un [Atom.Number] garde la notation de frappe **et** les chiffres bruts. C'est ce qui
 * remplace l'aller-retour `encode`/`decode` du code d'origine, qui perdait de l'information
 * dès qu'une décimale était en jeu : `decode(encode(x))` ne redonnait pas `x`.
 */
class AtomTest {

    private fun decimal(digits: String) = Atom.Number(NumberNotation.DECIMAL, digits)

    private fun shadok(digits: String) = Atom.Number(NumberNotation.SHADOK, digits)

    @ParameterizedTest(name = "\"{0}\" = {1}")
    @CsvSource(
        "0,      0",
        "7,      7",
        "42,     42",
        "0.5,    1/2",
        "0.25,   1/4",
        "12.34,  617/50",
        "007,    7",
    )
    fun `une saisie decimale devient une fraction exacte`(digits: String, expected: String) {
        // Exacte, et non un double : 12.34 vaut 617/50 au numérateur près, pas 12.339999…
        assertEquals(Rational.parseOrNull(expected), decimal(digits).value())
    }

    @ParameterizedTest(name = "\"{0}\" = {1}")
    @CsvSource(
        // ◯ Ga=0, _ Bu=1, ⅃ Zo=2, ◿ Meu=3 — en base 4.
        "◯,      0",
        "_,      1",
        "⅃,      2",
        "◿,      3",
        "_◯,     4",
        "_⅃,     6",
        "⅃⅃⅃,    42",
        // Fractionnaire : ◯.⅃ = 2×4⁻¹ = 1/2.
        "◯.⅃,    1/2",
        "◯._,    1/4",
    )
    fun `une saisie Shadok devient la meme fraction exacte`(digits: String, expected: String) {
        assertEquals(Rational.parseOrNull(expected), shadok(digits).value())
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["7.", "0.", "12."])
    fun `un separateur final en cours de frappe est ignore`(digits: String) {
        // L'état juste après l'appui sur le point : la valeur doit déjà être calculable,
        // sinon l'afficheur ne pourrait rien montrer entre le point et le chiffre suivant.
        assertEquals(
            Rational.parseOrNull(digits.trimEnd('.')),
            decimal(digits).value(),
        )
    }

    @Test
    fun `un separateur final Shadok est ignore aussi`() {
        assertEquals(Rational.of(1), shadok("_.").value())
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["", ".", "..."])
    fun `une saisie sans chiffre vaut zero plutot que de lever`(digits: String) {
        // Un tampon vide est l'état de départ de toute frappe : ce n'est pas une anomalie.
        // Ce sont les deux seuls états sans chiffre qu'ExpressionBuffer peut produire.
        assertEquals(Rational.ZERO, decimal(digits).value(), "\"$digits\"")
        assertEquals(Rational.ZERO, shadok(digits).value(), "\"$digits\" en Shadok")
    }

    @Test
    fun `une saisie Shadok illisible vaut zero, sans lever`() {
        // Ne devrait pas arriver — ExpressionBuffer filtre les chiffres selon la notation —
        // mais une valeur restaurée d'un stockage corrompu peut passer par ici.
        assertEquals(Rational.ZERO, shadok("42").value())
        assertEquals(Rational.ZERO, shadok("abc").value())
    }

    @ParameterizedTest(name = "\"{0}\" a un separateur : {1}")
    @CsvSource(
        "7,      false",
        "7.5,    true",
        "7.,     true",
        "0.25,   true",
        "'',     false",
    )
    fun `hasSeparator dit si le point a deja ete tape`(digits: String, expected: Boolean) {
        // C'est la garde qui empêche « 1.2.3 » : un seul séparateur par nombre.
        assertEquals(expected, decimal(digits).hasSeparator)
    }

    @Test
    fun `le separateur est le meme dans les deux notations`() {
        assertEquals('.', ShadokFormatter.SEPARATOR)
        assertTrue(shadok("◯.⅃").hasSeparator)
        assertFalse(shadok("◯⅃").hasSeparator)
    }

    @Test
    fun `toBase4 rend l'ecriture Shadok de la valeur`() {
        // 42 = 222 en base 4, quelle que soit la notation dans laquelle 42 a été tapé.
        val fromDecimal = decimal("42").toBase4()
        val fromShadok = shadok("⅃⅃⅃").toBase4()

        assertEquals(fromShadok, fromDecimal, "la notation de saisie ne change pas la valeur")
        assertEquals("ZoZoZo", ShadokFormatter.format(fromDecimal, ShadokNotation.LABELS))
    }

    @Test
    fun `toBase4 signale un developpement qui ne termine pas`() {
        // 0.1 décimal n'a pas d'écriture finie en base 4 : 4 = 2² ne divise pas 10.
        assertTrue(decimal("0.1").toBase4().approximate)
        // 0.25 en a une : Ga.Bu.
        assertFalse(decimal("0.25").toBase4().approximate)
    }

    @Test
    fun `la notation et les chiffres sont conserves tels quels`() {
        // Le point de conception : rien n'est normalisé à la construction, donc l'afficheur
        // peut restituer exactement ce qui a été tapé — zéros de tête compris.
        val typed = decimal("007.50")

        assertEquals(NumberNotation.DECIMAL, typed.notation)
        assertEquals("007.50", typed.digits)
        assertEquals(Rational.parseOrNull("15/2"), typed.value())
    }

    @Test
    fun `les atomes de structure sont des singletons comparables`() {
        // data object : deux parenthèses ouvrantes sont la même chose, et le `when` du
        // parseur peut les comparer par identité.
        assertEquals<Atom>(Atom.LeftParen, Atom.LeftParen)
        assertNotEquals<Atom>(Atom.LeftParen, Atom.RightParen)
        assertEquals<Atom>(Atom.Op(Operator.PLUS), Atom.Op(Operator.PLUS))
        assertNotEquals<Atom>(Atom.Op(Operator.PLUS), Atom.Op(Operator.MINUS))
    }
}
