package eu.ttbox.gabuzomeu.ui.calculator.components

import eu.ttbox.gabuzomeu.R
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.core.eval.Operator
import eu.ttbox.gabuzomeu.core.shadok.ShadokDigit
import eu.ttbox.gabuzomeu.ui.calculator.KeyAction
import eu.ttbox.gabuzomeu.ui.theme.KeyPalette

/** Rôle visuel d'une touche ; détermine sa couleur dans le thème Material. */
enum class KeyKind { DIGIT, OPERATOR, FUNCTION, EQUALS }

/**
 * Une touche du pavé.
 *
 * @property glyph le chiffre Shadok à dessiner, si la touche en est un. Sa description
 *   d'accessibilité vient alors de [ShadokDigit.label], donc TalkBack annonce « Zo » et
 *   non « ⅃ ». Le projet d'origine recopiait ces descriptions à la main dans le XML et
 *   les avait inversées entre Zo et Meu (`res/layout-port/shadok_pad.xml:32-42`).
 */
data class KeySpec(
    val action: KeyAction,
    val kind: KeyKind,
    val text: String? = null,
    val glyph: ShadokDigit? = null,
    val contentDescriptionRes: Int? = null,
    val weight: Float = 1f,
    /**
     * Le nom et la valeur écrits sous le glyphe — « Ga (0) ». Pavé Simple uniquement.
     *
     * C'est ce qui fait de ce pavé un pavé qui s'explique : on y apprend la base 4 en
     * regardant les touches. Ailleurs, la glose serait du bruit sur une touche qu'on
     * connaît déjà.
     */
    val subLabel: ShadokDigit? = null,
    /**
     * La palette de l'affiche des Shadoks. `null` : la touche prend les couleurs de son
     * [kind], comme partout ailleurs.
     */
    val palette: KeyPalette? = null,
)

/**
 * Dispositions du pavé.
 *
 * Un pavé par croisement mode de calcul × notation, décrit en données — plus de
 * `ViewPager`, plus de `PanelSwitcher`, et plus de tableaux `arrays.xml` dupliqués par
 * orientation. Le projet d'origine en avait quatre variantes (`values-port`, `values-land`,
 * `values-sw600dp`…) et la variante tablette avait **oublié les touches Shadok**, rendant
 * la fonctionnalité inaccessible sur grand écran.
 *
 * Décrits en données, six pavés ne coûtent pas plus cher que deux : le mode Simple n'a
 * demandé que deux listes de plus.
 *
 * Les pavés NPI n'ont ni parenthèses ni « = » : en postfixe, l'ordre de frappe est l'ordre
 * de calcul, il n'y a rien à grouper ni à déclencher. À leur place, le jeu des HP —
 * `ENTER` pour empiler, `x↔y` et `x↓` pour rattraper un ordre erroné, et `±` sans lequel
 * un opérande négatif serait insaisissable, faute de moins préfixe.
 */
object KeypadLayout {

    fun forMode(mode: CalculationMode, notation: NumberNotation): List<List<KeySpec>> =
        when (mode) {
            CalculationMode.SIMPLE -> when (notation) {
                NumberNotation.DECIMAL -> simpleDecimal
                NumberNotation.SHADOK -> simpleShadok
            }

            CalculationMode.CLASSIC -> when (notation) {
                NumberNotation.DECIMAL -> classicDecimal
                NumberNotation.SHADOK -> classicShadok
            }

            CalculationMode.RPN -> when (notation) {
                NumberNotation.DECIMAL -> rpnDecimal
                NumberNotation.SHADOK -> rpnShadok
            }
        }

    private val functionRow = listOf(
        KeySpec(
            action = KeyAction.Clear,
            kind = KeyKind.FUNCTION,
            text = "C",
            contentDescriptionRes = R.string.key_clear,
        ),
        KeySpec(
            action = KeyAction.Delete,
            kind = KeyKind.FUNCTION,
            text = "⌫",
            contentDescriptionRes = R.string.key_delete,
        ),
        KeySpec(
            action = KeyAction.LeftParen,
            kind = KeyKind.FUNCTION,
            text = "(",
            contentDescriptionRes = R.string.key_left_paren,
        ),
        KeySpec(
            action = KeyAction.RightParen,
            kind = KeyKind.FUNCTION,
            text = ")",
            contentDescriptionRes = R.string.key_right_paren,
        ),
    )

    private fun digit(character: Char) = KeySpec(
        action = KeyAction.Digit(character),
        kind = KeyKind.DIGIT,
        text = character.toString(),
    )

    private fun shadokDigit(digit: ShadokDigit) = KeySpec(
        action = KeyAction.Digit(digit.glyph),
        kind = KeyKind.DIGIT,
        glyph = digit,
    )

    private fun op(operator: Operator, descriptionRes: Int) = KeySpec(
        action = KeyAction.Op(operator),
        kind = KeyKind.OPERATOR,
        text = operator.symbol.toString(),
        contentDescriptionRes = descriptionRes,
    )

    private val separator = KeySpec(
        action = KeyAction.Separator,
        kind = KeyKind.DIGIT,
        text = ".",
        contentDescriptionRes = R.string.key_separator,
    )

    private val equals = KeySpec(
        action = KeyAction.Evaluate,
        kind = KeyKind.EQUALS,
        text = "=",
        contentDescriptionRes = R.string.key_equals,
    )

    // -------------------------------------------------------------- touches NPI

    /**
     * POMPER ↵ : empiler, dans les mots des Shadoks.
     *
     * Le même verbe qu'en mode Simple, et pour la même raison — c'est la touche par laquelle
     * on fait avancer la machine. Le mot anglais « ENTER » n'était de toute façon pas plus
     * parlant que « DROP », remplacé pour ce motif par `x↓`. Le `↵` reste : c'est lui qui dit
     * *où* ça pompe, à savoir vers la pile, là où le pavé Simple pompe vers un résultat.
     * La description d'accessibilité, elle, garde `key_enter` — « Empiler ».
     */
    private val enter = KeySpec(
        action = KeyAction.Enter,
        kind = KeyKind.EQUALS,
        text = "POMPER ↵",
        contentDescriptionRes = R.string.key_enter,
    )

    private val swap = KeySpec(
        action = KeyAction.Swap,
        kind = KeyKind.FUNCTION,
        text = "x↔y",
        contentDescriptionRes = R.string.key_swap,
    )

    private val drop = KeySpec(
        action = KeyAction.Drop,
        kind = KeyKind.FUNCTION,
        // « x↓ » et non « DROP » : le seul mot anglais du pavé, illisible pour qui ne le
        // connaît pas. Le couple se lit maintenant ensemble — `x↔y` échange x et y, `x↓`
        // fait sortir x — et « x » est la notation HP du sommet de pile. Le libellé
        // traduit reste dans `key_drop`, pour TalkBack.
        text = "x↓",
        contentDescriptionRes = R.string.key_drop,
    )

    private val negate = KeySpec(
        action = KeyAction.Negate,
        kind = KeyKind.OPERATOR,
        text = "±",
        contentDescriptionRes = R.string.key_negate,
    )

    private val divide = op(Operator.DIVIDE, R.string.key_divide)
    private val times = op(Operator.TIMES, R.string.key_times)
    private val minus = op(Operator.MINUS, R.string.key_minus)
    private val plus = op(Operator.PLUS, R.string.key_plus)

    // ------------------------------------------------------------- dispositions

    private val classicDecimal: List<List<KeySpec>> = listOf(
        functionRow,
        listOf(digit('7'), digit('8'), digit('9'), divide),
        listOf(digit('4'), digit('5'), digit('6'), times),
        listOf(digit('1'), digit('2'), digit('3'), minus),
        listOf(separator, digit('0'), equals, plus),
    )

    private val classicShadok: List<List<KeySpec>> = listOf(
        functionRow,
        listOf(shadokDigit(ShadokDigit.GA), shadokDigit(ShadokDigit.BU), divide, times),
        listOf(shadokDigit(ShadokDigit.ZO), shadokDigit(ShadokDigit.MEU), minus, plus),
        listOf(separator, equals.copy(weight = 3f)),
    )

    /** La rangée de fonctions en NPI : `x↔y` et `x↓` remplacent les parenthèses. */
    private val rpnFunctionRow = listOf(functionRow[0], functionRow[1], swap, drop)

    private val rpnDecimal: List<List<KeySpec>> = listOf(
        rpnFunctionRow,
        listOf(digit('7'), digit('8'), digit('9'), divide),
        listOf(digit('4'), digit('5'), digit('6'), times),
        listOf(digit('1'), digit('2'), digit('3'), minus),
        listOf(negate, digit('0'), separator, plus),
        // POMPER sur toute la largeur : c'est la touche la plus frappée en NPI, et elle
        // hérite de la place qu'occupait « = ».
        listOf(enter),
    )

    private val rpnShadok: List<List<KeySpec>> = listOf(
        rpnFunctionRow,
        listOf(shadokDigit(ShadokDigit.GA), shadokDigit(ShadokDigit.BU), divide, times),
        listOf(shadokDigit(ShadokDigit.ZO), shadokDigit(ShadokDigit.MEU), minus, plus),
        listOf(negate, separator, enter.copy(weight = 2f)),
    )

    // ------------------------------------------------------ touches du mode Simple

    /**
     * POMPER, c'est-à-dire « = » : la touche qui fait tourner la machine.
     *
     * « Il vaut mieux pomper même s'il ne se passe rien… » — la devise porte sur l'effort
     * qu'on fournit pour qu'il se passe quelque chose, donc sur le calcul, pas sur
     * l'effacement. Le libellé visible est le mot Shadok, non traduisible comme « x↔y » ou
     * « x↓ », d'où un littéral et non une ressource. La description d'accessibilité, en
     * revanche, reste `key_equals` : une plaisanterie ne doit pas rendre une touche
     * inintelligible à qui l'écoute plutôt que de la voir.
     */
    private val pomper = KeySpec(
        action = KeyAction.Evaluate,
        kind = KeyKind.EQUALS,
        text = "POMPER",
        contentDescriptionRes = R.string.key_equals,
        palette = KeyPalette.POMPER,
    )

    /** Le C des autres pavés, en rouge : ici, il est voisin du ⌫ et doit s'en distinguer. */
    private val simpleClear = functionRow[0].copy(palette = KeyPalette.CLEAR)

    private val simpleDelete = functionRow[1]

    /** Un chiffre Shadok glosé de son nom et de sa valeur, et teinté de sa couleur. */
    private fun simpleDigit(digit: ShadokDigit, palette: KeyPalette) = KeySpec(
        action = KeyAction.Digit(digit.glyph),
        kind = KeyKind.DIGIT,
        glyph = digit,
        subLabel = digit,
        palette = palette,
    )

    /**
     * Le pavé de l'affiche : quatre chiffres, quatre opérateurs, C, ⌫ et POMPER.
     *
     * Ni parenthèses — il n'y a pas d'expression à grouper — ni séparateur décimal : une
     * fraction s'obtient par une division, `Bu ÷ ⅃ =` donnant `Ga,⅃`. Le ⌫ ne figure pas
     * sur l'affiche ; sans lui, une faute de frappe obligerait à tout effacer.
     */
    private val simpleShadok: List<List<KeySpec>> = listOf(
        listOf(
            simpleDigit(ShadokDigit.GA, KeyPalette.GA),
            simpleDigit(ShadokDigit.BU, KeyPalette.BU),
            simpleDigit(ShadokDigit.ZO, KeyPalette.ZO),
            simpleDigit(ShadokDigit.MEU, KeyPalette.MEU),
        ),
        listOf(plus, minus, times, divide),
        listOf(simpleClear, simpleDelete, pomper),
    )

    /**
     * Le même mode, écrit en décimal.
     *
     * Dix chiffres au lieu de quatre, donc la disposition familière du pavé classique —
     * moins les parenthèses et le séparateur, que ce mode n'a pas.
     */
    private val simpleDecimal: List<List<KeySpec>> = listOf(
        listOf(simpleClear.copy(weight = 2f), simpleDelete.copy(weight = 2f)),
        listOf(digit('7'), digit('8'), digit('9'), divide),
        listOf(digit('4'), digit('5'), digit('6'), times),
        listOf(digit('1'), digit('2'), digit('3'), minus),
        listOf(digit('0').copy(weight = 2f), pomper, plus),
    )
}
