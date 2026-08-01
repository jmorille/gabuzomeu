package eu.ttbox.gabuzomeu.ui.calculator

import eu.ttbox.gabuzomeu.core.eval.SimpleOutcome
import eu.ttbox.gabuzomeu.core.eval.SimpleSession

/**
 * Le routage des touches du mode Simple, **hors du ViewModel**.
 *
 * Une fonction pure plutôt qu'une méthode de plus : [SimpleSession] étant immuable, le
 * ViewModel n'a qu'à retenir le résultat. Cela le garde aussi sous le seuil Detekt
 * `TooManyFunctions` (25 par classe), qu'un troisième mode de calcul aurait franchi — et
 * cela rend le routage testable sans instancier de ViewModel.
 *
 * Les touches absentes du pavé Simple ne sont pas oubliées pour autant : le `when` est
 * exhaustif, donc une action nouvelle devra passer par ici.
 */
internal fun SimpleSession.handle(action: KeyAction): SimpleOutcome = when (action) {
    is KeyAction.Digit -> SimpleOutcome(appendDigit(action.character))

    KeyAction.Separator -> SimpleOutcome(appendSeparator())

    // Les deux seules touches qui peuvent échouer — division par zéro — et qui rendent
    // alors la session d'avant, intacte.
    is KeyAction.Op -> operator(action.operator)

    KeyAction.Evaluate -> evaluate()

    KeyAction.Delete -> SimpleOutcome(deleteLast())

    KeyAction.Clear -> SimpleOutcome(clear())

    // Pas de touche ± sur ce pavé, mais l'action existe et a un sens ici : c'est par elle
    // qu'un nombre collé garde son signe.
    KeyAction.Negate -> SimpleOutcome(negate())

    // Le pavé Simple n'a ni parenthèses — il n'y a pas d'expression à grouper — ni les
    // touches de pile de la NPI.
    KeyAction.LeftParen,
    KeyAction.RightParen,
    KeyAction.Enter,
    KeyAction.Swap,
    KeyAction.Drop,
    -> SimpleOutcome(this)
}
