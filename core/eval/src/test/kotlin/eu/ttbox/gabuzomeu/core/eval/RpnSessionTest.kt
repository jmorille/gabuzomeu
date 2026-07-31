package eu.ttbox.gabuzomeu.core.eval

import eu.ttbox.gabuzomeu.core.shadok.Rational
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La grammaire de frappe en notation polonaise inverse.
 *
 * Tout est vérifiable sur la JVM, sans émulateur : [RpnSession] ne connaît ni `Context` ni
 * Compose, exactement comme [ExpressionBuffer] dont il réutilise les règles de saisie.
 */
class RpnSessionTest {

    // -------------------------------------------------------------- calcul de base

    @Test
    fun `six ENTER sept fois donne quarante-deux`() {
        val session = type("6").enter().type("7").applying(Operator.TIMES)

        assertEquals(Rational.of(42), session.stack.top)
        // 42 en base 4 s'ecrit 222, soit ZoZoZo.
        assertEquals("ZoZoZo", session.stackText(ExpressionDisplay.SHADOK_LABELS).single())
        // La frappe est consommee : l'afficheur principal montre la pile, pas un residu.
        assertFalse(session.entering)
    }

    @Test
    fun `le sommet est l'operande droit`() {
        assertEquals(
            Rational.of(7),
            type("10").enter().type("3").applying(Operator.MINUS).stack.top,
        )
        assertEquals(
            Rational.of(4),
            type("12").enter().type("3").applying(Operator.DIVIDE).stack.top,
        )
    }

    @Test
    fun `ENTER sur une frappe vide duplique le sommet`() {
        // La convention HP : ENTER × eleve au carre.
        val session = type("5").enter().enter().applying(Operator.TIMES)

        assertEquals(Rational.of(25), session.stack.top)
        assertEquals(1, session.stack.depth)
    }

    @Test
    fun `ENTER sur un etat entierement vide empile zero`() {
        val session = RpnSession().enter()

        assertEquals(Rational.ZERO, session.stack.top)
    }

    @Test
    fun `un chiffre apres un calcul demarre une frappe et laisse le resultat en pile`() {
        // 3 ENTER 4 + donne 7 ; puis 5 × doit donner 35, donc 7 est reste empile.
        val session = type("3").enter().type("4").applying(Operator.PLUS)
            .type("5").applying(Operator.TIMES)

        assertEquals(Rational.of(35), session.stack.top)
        assertEquals(1, session.stack.depth)
    }

    @Test
    fun `les operations s'enchainent sans parentheses ni priorite`() {
        // (2+3)×4 en NPI : 2 ENTER 3 + 4 × — l'ordre de frappe est l'ordre de calcul.
        val session = type("2").enter().type("3").applying(Operator.PLUS)
            .type("4").applying(Operator.TIMES)

        assertEquals(Rational.of(20), session.stack.top)
    }

    // ---------------------------------------------------------------------- pile

    @Test
    fun `x swap y echange les deux sommets`() {
        // 2 ENTER 7 x↔y − : 7 devient l'operande gauche, donc 7-2 = 5.
        val session = type("2").enter().type("7").swapping().applying(Operator.MINUS)

        assertEquals(Rational.of(5), session.stack.top)
    }

    @Test
    fun `DROP abandonne la frappe, puis depile`() {
        val entering = type("2").enter().type("99")

        val abandoned = entering.dropTop()
        assertFalse(abandoned.entering, "la frappe est abandonnee")
        assertEquals(Rational.of(2), abandoned.stack.top, "la pile est intacte")

        assertTrue(abandoned.dropTop().stack.isEmpty, "un second DROP depile")
    }

    @Test
    fun `l'effacement n'entame jamais la pile`() {
        val session = type("2").enter().type("34")

        val once = session.deleteLast()
        assertEquals("3", once.renderEntry(ExpressionDisplay.DECIMAL).text)

        val twice = once.deleteLast()
        assertFalse(twice.entering)
        // Trois effacements de plus : la pile ne bouge pas pour autant.
        assertEquals(
            Rational.of(2),
            twice.deleteLast().deleteLast().deleteLast().stack.top,
        )
    }

    @Test
    fun `C vide la frappe et la pile`() {
        val cleared = type("2").enter().type("7").clear()

        assertTrue(cleared.stack.isEmpty)
        assertFalse(cleared.entering)
    }

    // --------------------------------------------------------------------- signe

    @Test
    fun `le plus-ou-moins signe la frappe`() {
        val session = type("5").negate()

        assertTrue(session.entryNegative)
        assertEquals("−5", session.renderEntry(ExpressionDisplay.DECIMAL).text)
        // Et la valeur suit : 3 + (-5) = -2.
        assertEquals(
            Rational.of(-2),
            type("3").enter().type("5").negate().applying(Operator.PLUS).stack.top,
        )
    }

    @Test
    fun `le plus-ou-moins est reversible`() {
        assertFalse(type("5").negate().negate().entryNegative)
    }

    @Test
    fun `sans frappe, le plus-ou-moins signe le sommet`() {
        val session = type("5").enter().negate()

        assertEquals(Rational.of(-5), session.stack.top)
        assertFalse(session.entryNegative, "aucune frappe n'est en cours")
    }

    @Test
    fun `un signe ne survit pas a une frappe videe`() {
        // Sinon il serait invisible, et resurgirait au chiffre suivant.
        val emptied = type("5").negate().deleteLast()

        assertFalse(emptied.entering)
        assertFalse(emptied.entryNegative)
    }

    // -------------------------------------------------------------------- erreurs

    @Test
    fun `un operateur sur une pile trop courte ne detruit rien`() {
        val session = type("5")

        val outcome = session.apply(Operator.DIVIDE)

        assertEquals(EvalError.STACK_UNDERFLOW, outcome.error)
        // Le point important : la frappe est toujours la, l'utilisateur corrige.
        assertEquals(session, outcome.session)
    }

    @Test
    fun `un echange sur une pile trop courte ne detruit rien`() {
        val session = type("5")

        val outcome = session.swap()

        assertEquals(EvalError.STACK_UNDERFLOW, outcome.error)
        assertEquals(session, outcome.session)
    }

    @Test
    fun `la division par zero laisse les deux operandes en place`() {
        val session = type("5").enter().type("0")

        val outcome = session.apply(Operator.DIVIDE)

        assertEquals(EvalError.DIVISION_BY_ZERO, outcome.error)
        assertEquals(session, outcome.session)
        assertEquals(Rational.of(5), outcome.session.stack.top)
        assertEquals("0", outcome.session.renderEntry(ExpressionDisplay.DECIMAL).text)
    }

    // ------------------------------------------------------------------ exactitude

    @Test
    fun `un tiers empile reste exactement un tiers`() {
        val session = type("1").enter().type("3").applying(Operator.DIVIDE)

        // Trois tiers font exactement un : aucune derive flottante.
        assertEquals(Rational.ONE, session.stack.top!! * Rational.of(3))
        // Et les deux ecritures se declarent approchees, chacune pour sa raison.
        assertTrue(session.renderStack(ExpressionDisplay.DECIMAL).single().approximate)
        assertTrue(session.renderStack(ExpressionDisplay.SHADOK_GLYPHS).single().approximate)
    }

    @Test
    fun `un quart empile est exact dans les deux ecritures`() {
        // 1/4 = 4^-1 : fini en base 4 comme en base 10.
        val session = type("1").enter().type("4").applying(Operator.DIVIDE)

        assertFalse(session.renderStack(ExpressionDisplay.DECIMAL).single().approximate)
        assertFalse(session.renderStack(ExpressionDisplay.SHADOK_GLYPHS).single().approximate)
        assertEquals("Ga.Bu", session.stackText(ExpressionDisplay.SHADOK_LABELS).single())
        assertEquals("0.25", session.stackText(ExpressionDisplay.DECIMAL).single())
    }

    @Test
    fun `la pile se rend du fond vers le sommet`() {
        val session = type("1").enter().type("2").enter().type("3").enter()

        assertEquals(listOf("1", "2", "3"), session.stackText(ExpressionDisplay.DECIMAL))
    }

    // ------------------------------------------------------------------ registre X

    @Test
    fun `X est la frappe quand elle est en cours`() {
        val session = type("2").enter().type("7")

        assertEquals("7", session.renderX(ExpressionDisplay.DECIMAL).text)
        // Et la pile sous X ne compte plus 7 en double.
        assertEquals(listOf("2"), session.belowX(ExpressionDisplay.DECIMAL))
    }

    @Test
    fun `X est le sommet de la pile hors frappe`() {
        // Sans cette regle, la grande ligne resterait vide apres chaque operation : il n'y
        // a pas de « = » en NPI pour y ramener le resultat.
        val session = type("6").enter().type("7").applying(Operator.TIMES)

        assertEquals("42", session.renderX(ExpressionDisplay.DECIMAL).text)
        assertEquals(emptyList(), session.belowX(ExpressionDisplay.DECIMAL))
    }

    @Test
    fun `X est vide sur une session vierge`() {
        assertEquals("", RpnSession().renderX(ExpressionDisplay.DECIMAL).text)
    }

    @Test
    fun `X signale une valeur non representable exactement`() {
        val session = type("1").enter().type("3").applying(Operator.DIVIDE)

        assertTrue(session.renderX(ExpressionDisplay.DECIMAL).approximate)
        assertTrue(session.renderX(ExpressionDisplay.SHADOK_GLYPHS).approximate)
    }

    @Test
    fun `X descend d'un cran quand la frappe reprend`() {
        val computed = type("6").enter().type("7").applying(Operator.TIMES)

        val typing = computed.type("5")

        assertEquals("5", typing.renderX(ExpressionDisplay.DECIMAL).text)
        // 42 n'a pas disparu : il est passe sous X, pret a servir d'operande gauche.
        assertEquals(listOf("42"), typing.belowX(ExpressionDisplay.DECIMAL))
    }

    // -------------------------------------------------------------------- notation

    @Test
    fun `en Shadok seuls les glyphes sont acceptes`() {
        val session = RpnSession.of(NumberNotation.SHADOK).appendDigit('7')

        assertFalse(session.entering, "un 7 decimal n'existe pas en base 4")
        assertEquals("3", session.appendDigit(ShadokDigit.MEU.glyph).entryText())
    }

    @Test
    fun `MEU ENTER ZO fois donne BuZo`() {
        val session = RpnSession.of(NumberNotation.SHADOK)
            .appendDigit(ShadokDigit.MEU.glyph).enter()
            .appendDigit(ShadokDigit.ZO.glyph)
            .applying(Operator.TIMES)

        // 3 x 2 = 6, et 6 en base 4 s'ecrit 12, soit BuZo.
        assertEquals(Rational.of(6), session.stack.top)
        assertEquals("BuZo", session.stackText(ExpressionDisplay.SHADOK_LABELS).single())
    }

    @Test
    fun `changer de notation convertit la frappe et laisse la pile telle quelle`() {
        val session = type("2").enter().type("6").withNotation(NumberNotation.SHADOK)

        assertEquals(NumberNotation.SHADOK, session.notation)
        // 6 en base 4 = 12, soit les glyphes Bu Zo.
        assertEquals("_⅃", session.renderEntry(ExpressionDisplay.SHADOK_GLYPHS).text)
        // La pile porte des Rational : il n'y a rien a convertir.
        assertEquals(listOf("2"), session.stackText(ExpressionDisplay.DECIMAL))
    }

    @Test
    fun `le signe de la frappe survit au changement de notation`() {
        val session = type("6").negate().withNotation(NumberNotation.SHADOK)

        assertTrue(session.entryNegative)
        assertEquals("−_⅃", session.renderEntry(ExpressionDisplay.SHADOK_GLYPHS).text)
    }

    // ----------------------------------------------------------------- persistance

    @Test
    fun `l'aller-retour de persistance reconstruit la session a l'identique`() {
        val session = type("1").enter().type("3").applying(Operator.DIVIDE)
            .type("42").negate()

        val restored = RpnSession.restore(
            stackKeys = session.stack.keys(),
            entryKeys = session.entryKeys(),
            entryNegative = session.entryNegative,
            notation = session.notation,
        )

        assertEquals(session, restored)
    }

    @Test
    fun `une session vide se persiste et se relit`() {
        val restored = RpnSession.restore("", "", entryNegative = false, NumberNotation.DECIMAL)

        assertTrue(restored.stack.isEmpty)
        assertFalse(restored.entering)
    }

    @Test
    fun `une frappe stockee corrompue ne peut pas violer l'invariant`() {
        // « 1+2 » n'est pas une frappe NPI valide : on ne garde que le premier nombre.
        val restored = RpnSession.restore("", "1+2", entryNegative = false, NumberNotation.DECIMAL)

        assertEquals(1, restored.entry.atoms.size)
        assertEquals("1", restored.entryText())
    }

    @Test
    fun `un signe stocke sans chiffres est ignore`() {
        val restored = RpnSession.restore("5", "", entryNegative = true, NumberNotation.DECIMAL)

        assertFalse(restored.entryNegative)
        assertNull(restored.entry.atoms.firstOrNull())
    }

    // ------------------------------------------------------------------ utilitaires

    private fun type(digits: String): RpnSession = RpnSession().type(digits)

    private fun RpnSession.type(digits: String): RpnSession = digits.fold(this) { session, digit ->
        if (digit == '.') session.appendSeparator() else session.appendDigit(digit)
    }

    /** [apply] en supposant le succès : l'échec est vérifié explicitement ailleurs. */
    private fun RpnSession.applying(operator: Operator): RpnSession {
        val outcome = apply(operator)
        assertNull(outcome.error, "${operator.symbol} ne devrait pas echouer ici")
        return outcome.session
    }

    private fun RpnSession.swapping(): RpnSession {
        val outcome = swap()
        assertNull(outcome.error, "x↔y ne devrait pas echouer ici")
        return outcome.session
    }

    private fun RpnSession.entryText(): String = renderEntry(ExpressionDisplay.DECIMAL).text

    private fun RpnSession.stackText(display: ExpressionDisplay): List<String> =
        renderStack(display).map { it.text }

    private fun RpnSession.belowX(display: ExpressionDisplay): List<String> =
        renderBelowX(display).map { it.text }
}
