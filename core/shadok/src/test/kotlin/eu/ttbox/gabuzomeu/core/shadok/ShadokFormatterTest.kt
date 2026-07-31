package eu.ttbox.gabuzomeu.core.shadok

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Les trois écritures d'un même nombre.
 *
 * `ShadokConverterTest` vérifie déjà abondamment le *contenu* des conversions ; ce qui est
 * en jeu ici est la **mise en forme** : l'ordre des préfixes, le séparateur, et le fait que
 * les trois notations décrivent rigoureusement les mêmes chiffres.
 */
class ShadokFormatterTest {

    /** 6.25 en base 4 = 12.1 : deux chiffres entiers, un fractionnaire, rien d'ambigu. */
    private val sixAndAQuarter = Base4Number(
        negative = false,
        integerDigits = listOf(ShadokDigit.BU, ShadokDigit.ZO),
        fractionDigits = listOf(ShadokDigit.BU),
    )

    @Test
    fun `les trois notations decrivent les memes chiffres`() {
        assertEquals("_⅃._", ShadokFormatter.format(sixAndAQuarter, ShadokNotation.GLYPHS))
        assertEquals("BuZo.Bu", ShadokFormatter.format(sixAndAQuarter, ShadokNotation.LABELS))
        assertEquals("12.1", ShadokFormatter.format(sixAndAQuarter, ShadokNotation.BASE4))
    }

    @Test
    fun `un entier n'a pas de separateur`() {
        val seven = Base4Number(
            negative = false,
            integerDigits = listOf(ShadokDigit.BU, ShadokDigit.MEU),
        )

        assertEquals("BuMeu", ShadokFormatter.format(seven, ShadokNotation.LABELS))
        assertFalse(
            ShadokFormatter.format(seven, ShadokNotation.LABELS)
                .contains(ShadokFormatter.SEPARATOR),
        )
    }

    @Test
    fun `le moins est le vrai signe mathematique, pas un trait d'union`() {
        val minusSeven = Base4Number(
            negative = true,
            integerDigits = listOf(ShadokDigit.BU, ShadokDigit.MEU),
        )
        val rendered = ShadokFormatter.format(minusSeven, ShadokNotation.LABELS)

        assertEquals("−BuMeu", rendered)
        // U+2212, et surtout PAS le '-' ASCII : c'est la convention d'affichage du projet.
        assertEquals('−', ShadokFormatter.MINUS)
        assertFalse(rendered.contains('-'), "le trait d'union ASCII n'a rien à faire ici")
    }

    @Test
    fun `le marqueur d'approximation precede le signe`() {
        // L'ordre compte : « ≈−Bu » se lit « environ moins un ». « −≈Bu » ne se lit pas.
        val truncatedNegative = Base4Number(
            negative = true,
            integerDigits = listOf(ShadokDigit.GA),
            fractionDigits = listOf(ShadokDigit.BU),
            approximate = true,
        )

        assertEquals("≈−Ga.Bu", ShadokFormatter.format(truncatedNegative, ShadokNotation.LABELS))
    }

    @Test
    fun `le marqueur d'approximation se desactive pour comparer les chiffres`() {
        val truncated = Base4Number(
            negative = false,
            integerDigits = listOf(ShadokDigit.GA),
            fractionDigits = listOf(ShadokDigit.BU),
            approximate = true,
        )

        assertEquals(
            "Ga.Bu",
            ShadokFormatter.format(truncated, ShadokNotation.LABELS, markApproximation = false),
        )
    }

    @Test
    fun `un nombre exact ne porte jamais le marqueur, meme si on le demande`() {
        // markApproximation n'AJOUTE pas d'approximation : il autorise seulement à la signaler.
        val exact = ShadokConverter.toBase4(Rational.ofDecimal("0.5"))

        assertFalse(exact.approximate)
        assertEquals(
            "Ga.Zo",
            ShadokFormatter.format(exact, ShadokNotation.LABELS, markApproximation = true),
        )
    }

    @Test
    fun `le marqueur est signale par defaut`() {
        // Le défaut est le cas de l'affichage : mieux vaut prévenir que rendre un faux exact.
        val third = ShadokConverter.toBase4(Rational.parseOrNull("1/3")!!)

        assertTrue(third.approximate)
        assertTrue(
            ShadokFormatter.format(third, ShadokNotation.LABELS)
                .startsWith(ShadokFormatter.APPROXIMATION),
        )
    }

    @ParameterizedTest
    @EnumSource(ShadokNotation::class)
    fun `zero se rend dans les trois notations sans signe`(notation: ShadokNotation) {
        val zero = ShadokConverter.toBase4(Rational.ZERO)

        val rendered = ShadokFormatter.format(zero, notation)
        assertFalse(rendered.contains(ShadokFormatter.MINUS), "zéro n'est jamais négatif")
        assertFalse(rendered.contains(ShadokFormatter.SEPARATOR), "zéro est un entier")
        // Le seul chiffre attendu est Ga, quelle que soit l'écriture demandée.
        assertEquals(
            ShadokFormatter.format(
                Base4Number(negative = false, integerDigits = listOf(ShadokDigit.GA)),
                notation,
            ),
            rendered,
        )
    }

    @Test
    fun `la notation BASE4 sert au diagnostic et reste lisible`() {
        // 42 = 222 en base 4 : la forme brute est celle qu'on veut voir dans un message d'erreur.
        val fortyTwo = ShadokConverter.toBase4(Rational.of(42))

        assertEquals("222", ShadokFormatter.format(fortyTwo, ShadokNotation.BASE4))
        assertEquals("ZoZoZo", ShadokFormatter.format(fortyTwo, ShadokNotation.LABELS))
    }
}
