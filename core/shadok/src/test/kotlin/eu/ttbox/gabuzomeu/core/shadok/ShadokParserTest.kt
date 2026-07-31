package eu.ttbox.gabuzomeu.core.shadok

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La relecture d'une saisie en glyphes, et celle des noms prononcés.
 *
 * Deux exigences se croisent ici. D'abord, une entrée invalide rend `null` et ne **lève
 * jamais** : pendant une frappe, « ◯. » ou « − » sont des états transitoires parfaitement
 * normaux, pas des anomalies. Ensuite, la lecture doit être l'inverse exact de
 * [ShadokFormatter] — c'est ce couple qui fait qu'un nombre affiché puis relu est le même.
 */
class ShadokParserTest {

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(
        // Entiers : les glyphes se lisent du plus significatif au moins significatif.
        "◯,      Ga",
        "_,      Bu",
        "⅃,      Zo",
        "◿,      Meu",
        "_◯,     BuGa",
        "_⅃,     BuZo",
        "⅃⅃⅃,    ZoZoZo",
        // Fractions.
        "◯._,    Ga.Bu",
        "◯.⅃,    Ga.Zo",
        "_⅃._,   BuZo.Bu",
        // Zéros de tête conservés : la relecture est fidèle à ce qui a été tapé.
        "◯◯_,    GaGaBu",
    )
    fun `un nombre bien forme se relit`(text: String, expectedLabels: String) {
        val parsed = assertNotNull(ShadokParser.parseGlyphsOrNull(text), "\"$text\"")

        assertEquals(
            expectedLabels,
            ShadokFormatter.format(parsed, ShadokNotation.LABELS),
        )
    }

    @Test
    fun `un separateur en tete vaut un Ga implicite`() {
        // « .⅃ » est ce que produit une frappe qui commence par le séparateur.
        val parsed = assertNotNull(ShadokParser.parseGlyphsOrNull(".⅃"))

        assertEquals(listOf(ShadokDigit.GA), parsed.integerDigits)
        assertEquals(listOf(ShadokDigit.ZO), parsed.fractionDigits)
    }

    @Test
    fun `un separateur final est accepte et ne laisse pas de chiffre`() {
        // L'état exact de la frappe juste après avoir appuyé sur le point.
        val parsed = assertNotNull(ShadokParser.parseGlyphsOrNull("◯."))

        assertEquals(listOf(ShadokDigit.GA), parsed.integerDigits)
        assertTrue(parsed.fractionDigits.isEmpty())
        assertTrue(parsed.isZero)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["−_⅃", "-_⅃"])
    fun `le signe est accepte sous ses deux formes`(text: String) {
        // U+2212 est ce qu'affiche la calculatrice ; le '-' ASCII est ce que tape un clavier.
        val parsed = assertNotNull(ShadokParser.parseGlyphsOrNull(text))

        assertTrue(parsed.negative)
        assertEquals(listOf(ShadokDigit.BU, ShadokDigit.ZO), parsed.integerDigits)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            // Aucun glyphe du tout : ni le signe seul, ni le séparateur seul ne sont un nombre.
            "", " ", "−", "-", ".", "−.", "42", "abc",
            // Un caractère étranger au milieu ou en fin laisse un reliquat non consommé.
            "_x", "x_", "_⅃abc", "_ ⅃", "_,⅃",
            // Deux séparateurs : le second n'est pas consommable.
            "◯._._", "◯..", "_.⅃.◿",
            // Le signe doit être en tête, pas ailleurs.
            "_−⅃", "_⅃−",
        ],
    )
    fun `une entree mal formee rend null sans lever`(text: String) {
        assertNull(ShadokParser.parseGlyphsOrNull(text), "\"$text\" ne devrait pas s'analyser")
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["0", "1", "6", "42", "255", "-7", "0.5", "0.25", "6.25", "-2.75"])
    fun `formater puis relire est l'identite`(decimal: String) {
        // La propriété qui compte : l'affichage ne perd rien de ce qu'il montre.
        val original = ShadokConverter.toBase4(Rational.ofDecimal(decimal))
        val text = ShadokFormatter.format(original, ShadokNotation.GLYPHS)

        val reread = assertNotNull(ShadokParser.parseGlyphsOrNull(text), "relecture de \"$text\"")

        assertEquals(original, reread, "aller-retour de $decimal par \"$text\"")
        assertEquals(Rational.ofDecimal(decimal), ShadokConverter.toRational(reread))
    }

    @Test
    fun `la relecture ne pretend jamais etre approximative`() {
        // Le parseur lit des chiffres ; il ne peut pas savoir si l'écriture était tronquée.
        // C'est pourquoi format() d'un nombre approché doit passer markApproximation = false
        // avant d'être relu — le marqueur « ≈ » n'est pas un caractère analysable.
        val third = ShadokConverter.toBase4(Rational.parseOrNull("1/3")!!)
        val withMarker = ShadokFormatter.format(third, ShadokNotation.GLYPHS)
        val withoutMarker =
            ShadokFormatter.format(third, ShadokNotation.GLYPHS, markApproximation = false)

        assertNull(ShadokParser.parseGlyphsOrNull(withMarker), "le « ≈ » n'est pas un chiffre")

        val reread = assertNotNull(ShadokParser.parseGlyphsOrNull(withoutMarker))
        assertFalse(reread.approximate)
        assertEquals(third.fractionDigits, reread.fractionDigits)
    }

    // ------------------------------------------------------- la relecture des noms

    @ParameterizedTest(name = "\"{0}\" -> {1}")
    @CsvSource(
        "Ga,        Ga",
        "Bu,        Bu",
        "Zo,        Zo",
        "Meu,       Meu",
        "BuZo,      BuZo",
        "ZoZoZo,    ZoZoZo",
        "MeuGa,     MeuGa",
        // Fractions et zéros de tête, comme pour les glyphes.
        "Ga.Zo,     Ga.Zo",
        "BuZo.Bu,   BuZo.Bu",
        "GaGaBu,    GaGaBu",
    )
    fun `un nombre ecrit en noms se relit`(text: String, expectedLabels: String) {
        val parsed = assertNotNull(ShadokParser.parseLabelsOrNull(text), "\"$text\"")

        assertEquals(expectedLabels, ShadokFormatter.format(parsed, ShadokNotation.LABELS))
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["BuZo", "buzo", "BUZO", "bUzO", "buZO"])
    fun `la casse des noms est libre`(text: String) {
        // Un nombre recopié depuis un message arrive volontiers en majuscules : le refuser
        // n'apporterait aucune sécurité, seulement un collage qui échoue sans raison visible.
        val parsed = assertNotNull(ShadokParser.parseLabelsOrNull(text), "\"$text\"")

        assertEquals(listOf(ShadokDigit.BU, ShadokDigit.ZO), parsed.integerDigits)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(strings = ["−BuZo", "-BuZo", "−BuZo.Meu"])
    fun `le signe des noms est accepte sous ses deux formes`(text: String) {
        assertTrue(assertNotNull(ShadokParser.parseLabelsOrNull(text)).negative)
    }

    @ParameterizedTest(name = "\"{0}\"")
    @ValueSource(
        strings = [
            // Aucun nom : ni vide, ni signe seul, ni séparateur seul.
            "", " ", "−", ".", "−.",
            // Des chiffres ne sont pas des noms : c'est ce qui distingue les deux lectures.
            "42", "12", "abc",
            // Un nom inexistant, ou tronqué, laisse un reliquat non consommé.
            "BuXo", "Meuu", "Gaa", "M", "Bu Zo", "Bu-Zo",
            // Les écritures ne se mélangent pas.
            "Bu⅃", "⅃Bu",
            // Deux séparateurs, et un signe hors de la tête.
            "Ga..", "Bu.Zo.Meu", "Bu−Zo", "BuZo−",
        ],
    )
    fun `une entree mal formee en noms rend null sans lever`(text: String) {
        assertNull(ShadokParser.parseLabelsOrNull(text), "\"$text\" ne devrait pas s'analyser")
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = ["0", "1", "6", "42", "255", "-7", "0.5", "0.25", "6.25", "-2.75"])
    fun `formater en noms puis relire est l'identite`(decimal: String) {
        val original = ShadokConverter.toBase4(Rational.ofDecimal(decimal))
        val text = ShadokFormatter.format(original, ShadokNotation.LABELS)

        val reread = assertNotNull(ShadokParser.parseLabelsOrNull(text), "relecture de \"$text\"")

        assertEquals(original, reread, "aller-retour de $decimal par \"$text\"")
        assertEquals(Rational.ofDecimal(decimal), ShadokConverter.toRational(reread))
    }

    @Test
    fun `les deux lectures ne se confondent pas`() {
        // Chacune refuse l'écriture de l'autre : c'est ce qui permet à un appelant d'essayer
        // les deux dans l'ordre et de savoir laquelle a répondu.
        assertNull(ShadokParser.parseLabelsOrNull("_⅃"))
        assertNull(ShadokParser.parseGlyphsOrNull("BuZo"))
    }
}
