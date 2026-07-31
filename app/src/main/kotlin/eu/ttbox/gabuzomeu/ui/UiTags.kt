package eu.ttbox.gabuzomeu.ui

import eu.ttbox.gabuzomeu.ui.calculator.KeyAction

/**
 * Repères de test de l'interface, réunis en un seul endroit.
 *
 * Ils sont **stables et indépendants de la langue** — contrairement aux libellés et aux
 * descriptions d'accessibilité, qui sont traduits. Deux mésaventures ont motivé leur
 * existence :
 *
 * - des assertions sur des chaînes françaises en dur passaient sur un téléphone français
 *   et échouaient sur l'émulateur `en-US` de la CI ;
 * - les touches du pavé appliquent `clearAndSetSemantics`, ce qui efface la sémantique de
 *   texte de leurs descendants : elles sont donc introuvables par `onNodeWithText`.
 */
object DisplayTags {
    const val GLYPHS = "display-glyphs"
    const val LABELS = "display-labels"
    const val DECIMAL = "display-decimal"

    /** La zone de pile NPI, présente seulement dans ce mode. */
    const val STACK = "display-stack"

    /** Un niveau de pile, repéré par son rang depuis le fond. */
    fun stackLevel(level: Int): String = "display-stack-$level"
}

object SettingsTags {
    const val MENU_BUTTON = "settings-menu"
    const val MODE_CLASSIC = "mode-classic"
    const val MODE_RPN = "mode-rpn"
    const val TOGGLE_LABELS = "toggle-shadok-labels"
    const val TOGGLE_DECIMAL = "toggle-decimal"
}

object KeypadTags {
    /** Dérivé de l'action : unique, stable, et lisible dans un rapport d'échec. */
    fun of(action: KeyAction): String = when (action) {
        is KeyAction.Digit -> "key-digit-${action.character.code}"
        is KeyAction.Op -> "key-op-${action.operator.name}"
        KeyAction.Separator -> "key-separator"
        KeyAction.LeftParen -> "key-left-paren"
        KeyAction.RightParen -> "key-right-paren"
        KeyAction.Delete -> "key-delete"
        KeyAction.Clear -> "key-clear"
        KeyAction.Evaluate -> "key-equals"
        KeyAction.Enter -> "key-enter"
        KeyAction.Swap -> "key-swap"
        KeyAction.Drop -> "key-drop"
        KeyAction.Negate -> "key-negate"
    }
}
