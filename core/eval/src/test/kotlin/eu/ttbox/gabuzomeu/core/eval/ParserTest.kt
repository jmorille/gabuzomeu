package eu.ttbox.gabuzomeu.core.eval

import eu.ttbox.gabuzomeu.core.shadok.Rational
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

/**
 * La grammaire, vérifiée sur l'**arbre** et non sur le résultat.
 *
 * ```
 * expression := terme      (('+' | '−') terme)*
 * terme      := facteur    (('×' | '÷') facteur)*
 * facteur    := '−' facteur | primaire
 * primaire   := nombre | '(' expression ')'
 * ```
 *
 * `EvaluatorTest` couvre déjà les valeurs calculées. Ce qui se joue ici est la **forme** :
 * c'est la structure de l'arbre qui porte la priorité et l'associativité, depuis que
 * `Operator` n'a plus de champ `precedence`. Une régression sur l'associativité à gauche
 * resterait invisible sur `2+3+4`, dont les deux arbres donnent 9 — mais pas sur `2−3−4`.
 *
 * On attaque [Parser] directement, avec des listes d'atomes écrites à la main : cela permet
 * d'atteindre des séquences qu'[ExpressionBuffer] refuserait de produire, et d'observer les
 * erreurs de syntaxe là où elles naissent plutôt qu'après le rattrapage d'[Evaluator].
 */
class ParserTest {

    private val plus: Atom = Atom.Op(Operator.PLUS)
    private val minus: Atom = Atom.Op(Operator.MINUS)
    private val times: Atom = Atom.Op(Operator.TIMES)
    private val divide: Atom = Atom.Op(Operator.DIVIDE)
    private val open: Atom = Atom.LeftParen
    private val close: Atom = Atom.RightParen

    private fun number(digits: String): Atom = Atom.Number(NumberNotation.DECIMAL, digits)

    private fun literal(value: Int): Expr = Expr.Literal(Rational.of(value))

    private fun parse(vararg atoms: Atom): Expr = Parser(atoms.toList()).parse()

    private fun errorOf(vararg atoms: Atom): EvalError =
        assertThrows<ParseException> { Parser(atoms.toList()).parse() }.error

    @Test
    fun `un nombre seul est une feuille`() {
        assertEquals(literal(42), parse(number("42")))
    }

    @Test
    fun `la soustraction est associative a gauche`() {
        // 2−3−4 doit être (2−3)−4 = −5, et non 2−(3−4) = 3. C'est la boucle de
        // parseExpression, qui accumule vers la gauche, qui le garantit.
        assertEquals(
            Expr.Binary(
                Expr.Binary(literal(2), Operator.MINUS, literal(3)),
                Operator.MINUS,
                literal(4),
            ),
            parse(number("2"), minus, number("3"), minus, number("4")),
        )
    }

    @Test
    fun `la division est associative a gauche`() {
        // 100÷10÷2 = 5, et non 100÷(10÷2) = 20.
        assertEquals(
            Expr.Binary(
                Expr.Binary(literal(100), Operator.DIVIDE, literal(10)),
                Operator.DIVIDE,
                literal(2),
            ),
            parse(number("100"), divide, number("10"), divide, number("2")),
        )
    }

    @Test
    fun `la multiplication lie plus fort que l'addition`() {
        // 2+3×4 : le × descend d'un niveau de grammaire, donc il se retrouve plus bas dans
        // l'arbre — c'est exactement ce que « priorité » veut dire ici.
        assertEquals(
            Expr.Binary(
                literal(2),
                Operator.PLUS,
                Expr.Binary(literal(3), Operator.TIMES, literal(4)),
            ),
            parse(number("2"), plus, number("3"), times, number("4")),
        )
    }

    @Test
    fun `la multiplication a gauche descend aussi`() {
        // 2×3+4 : le × est le fils GAUCHE du +, pas l'inverse.
        assertEquals(
            Expr.Binary(
                Expr.Binary(literal(2), Operator.TIMES, literal(3)),
                Operator.PLUS,
                literal(4),
            ),
            parse(number("2"), times, number("3"), plus, number("4")),
        )
    }

    @Test
    fun `les parentheses renversent la priorite`() {
        // (2+3)×4 : le + remonte au-dessus du ×.
        assertEquals(
            Expr.Binary(
                Expr.Binary(literal(2), Operator.PLUS, literal(3)),
                Operator.TIMES,
                literal(4),
            ),
            parse(open, number("2"), plus, number("3"), close, times, number("4")),
        )
    }

    @Test
    fun `les parentheses redondantes ne laissent aucune trace`() {
        // ((5)) et 5 donnent le même arbre : la parenthèse n'est pas un nœud.
        assertEquals(literal(5), parse(open, open, number("5"), close, close))
    }

    @Test
    fun `le moins unaire est un noeud de negation`() {
        assertEquals(Expr.Negate(literal(5)), parse(minus, number("5")))
    }

    @Test
    fun `un moins unaire peut suivre un operateur binaire`() {
        // 5+−3 : la saisie que la règle 3 du tampon autorise explicitement. Le moins est
        // traité au niveau `facteur`, ce qui rend la séquence analysable.
        assertEquals(
            Expr.Binary(literal(5), Operator.PLUS, Expr.Negate(literal(3))),
            parse(number("5"), plus, minus, number("3")),
        )
    }

    @Test
    fun `le moins unaire se repete`() {
        // `facteur := '−' facteur` est récursive : −−5 est une double négation, donc 5.
        assertEquals(
            Expr.Negate(Expr.Negate(literal(5))),
            parse(minus, minus, number("5")),
        )
    }

    @Test
    fun `le moins unaire se ferme avant la multiplication`() {
        // −2×3 donne (−2)×3 et non −(2×3) : le moins est consommé au niveau `facteur`,
        // c'est-à-dire SOUS le niveau `terme` où vit le ×. Les deux formes valent −6, donc
        // seule l'inspection de l'arbre peut faire la différence.
        assertEquals(
            Expr.Binary(Expr.Negate(literal(2)), Operator.TIMES, literal(3)),
            parse(minus, number("2"), times, number("3")),
        )
    }

    @Test
    fun `un moins unaire s'applique a une parenthese entiere`() {
        assertEquals(
            Expr.Negate(Expr.Binary(literal(2), Operator.PLUS, literal(3))),
            parse(minus, open, number("2"), plus, number("3"), close),
        )
    }

    // ------------------------------------------------------------------ erreurs

    @Test
    fun `une liste vide est une erreur de syntaxe pour le parseur seul`() {
        assertEquals(EvalError.SYNTAX, errorOf())

        // Mais ce n'est PAS ce que voit l'utilisateur : Evaluator intercepte le cas vide
        // avant d'appeler le parseur et rend EMPTY, qui porte un message adapté. Sans cette
        // seconde assertion, le test laisserait croire qu'appuyer sur « = » sur un écran
        // vierge affiche une erreur de syntaxe.
        assertEquals(EvalResult.Failure(EvalError.EMPTY), Evaluator.evaluate(emptyList()))
    }

    @Test
    fun `un operateur binaire sans operande droit est une erreur`() {
        // Le parseur n'a pas à être indulgent : c'est Evaluator qui coupe la queue des
        // opérateurs en attente avant de l'appeler.
        assertEquals(EvalError.SYNTAX, errorOf(number("5"), plus))
    }

    @Test
    fun `un operateur binaire sans operande gauche est une erreur`() {
        assertEquals(EvalError.SYNTAX, errorOf(times, number("5")))
    }

    @Test
    fun `un moins seul n'est pas analysable`() {
        assertEquals(EvalError.SYNTAX, errorOf(minus))
    }

    @Test
    fun `une fermante orpheline est une erreur de syntaxe`() {
        // Il reste un atome non consommé après l'expression : `position != atoms.size`.
        assertEquals(EvalError.SYNTAX, errorOf(number("5"), close))
    }

    @Test
    fun `deux nombres cote a cote sont une erreur`() {
        // Rien dans la grammaire n'autorise la juxtaposition : pas de produit implicite.
        assertEquals(EvalError.SYNTAX, errorOf(number("5"), number("3")))
    }

    @Test
    fun `une parenthese vide est une erreur de syntaxe`() {
        assertEquals(EvalError.SYNTAX, errorOf(open, close))
    }

    @Test
    fun `une ouvrante jamais fermee est un desequilibre pour le parseur seul`() {
        assertEquals(EvalError.UNBALANCED_PARENTHESES, errorOf(open, number("5")))

        // Là encore, l'utilisateur ne voit pas cette erreur : Evaluator complète les
        // parenthèses manquantes avant d'analyser, donc « (5 » vaut 5. C'est le
        // comportement de la calculatrice AOSP dont ce projet est issu.
        assertEquals(
            EvalResult.Success(Rational.of(5)),
            Evaluator.evaluate(listOf(open, number("5"))),
        )
    }

    @Test
    fun `une ouvrante suivie d'une autre non fermee est un desequilibre`() {
        assertEquals(
            EvalError.UNBALANCED_PARENTHESES,
            errorOf(open, number("5"), open),
        )
    }
}
