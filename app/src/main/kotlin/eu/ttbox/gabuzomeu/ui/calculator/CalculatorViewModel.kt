package eu.ttbox.gabuzomeu.ui.calculator

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import eu.ttbox.gabuzomeu.core.eval.Atom
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.ClipboardNumber
import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.eval.EvalResult
import eu.ttbox.gabuzomeu.core.eval.Evaluator
import eu.ttbox.gabuzomeu.core.eval.ExpressionBuffer
import eu.ttbox.gabuzomeu.core.eval.ExpressionDisplay
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.core.eval.Operator
import eu.ttbox.gabuzomeu.core.eval.PastedNumber
import eu.ttbox.gabuzomeu.core.eval.Rendered
import eu.ttbox.gabuzomeu.core.eval.RpnOutcome
import eu.ttbox.gabuzomeu.core.eval.RpnSession
import eu.ttbox.gabuzomeu.core.shadok.ShadokFormatter
import eu.ttbox.gabuzomeu.data.CalculatorPreferences
import eu.ttbox.gabuzomeu.data.DisplaySettings
import eu.ttbox.gabuzomeu.data.SessionStore
import eu.ttbox.gabuzomeu.data.StoredRpn
import eu.ttbox.gabuzomeu.data.StoredSession
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Ce qu'une touche du pavé demande de faire.
 *
 * Certaines actions n'ont de sens que dans un mode : les parenthèses et le `=` sont propres
 * à l'infixe, [Enter], [Swap], [Drop] et [Negate] à la NPI. Le pavé n'affiche jamais une
 * touche hors de son mode, mais le `when` du ViewModel reste exhaustif — c'est le
 * compilateur qui garantit qu'aucune action ne tombe dans l'oubli.
 */
sealed interface KeyAction {
    data class Digit(val character: Char) : KeyAction
    data class Op(val operator: Operator) : KeyAction
    data object Separator : KeyAction
    data object LeftParen : KeyAction
    data object RightParen : KeyAction
    data object Delete : KeyAction
    data object Clear : KeyAction
    data object Evaluate : KeyAction

    /** NPI : empile la frappe, ou duplique le sommet. */
    data object Enter : KeyAction

    /** NPI : échange les deux valeurs du sommet. */
    data object Swap : KeyAction

    /** NPI : abandonne la frappe, ou dépile le sommet. */
    data object Drop : KeyAction

    /** NPI : change le signe de la frappe, ou du sommet. */
    data object Negate : KeyAction
}

class CalculatorViewModel(
    private val sessionStore: SessionStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var mode = CalculationMode.CLASSIC

    /** Mode infixe. Conservé même quand la NPI est affichée : basculer ne détruit rien. */
    private var buffer = ExpressionBuffer()

    /** Mode NPI. Conservé de même, avec sa pile et sa frappe en cours. */
    private var rpn = RpnSession()

    private var showingResult = false
    private var error: EvalError? = null
    private var decimalApproximate = false
    private var settings = DisplaySettings()

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    /** Écritures disque regroupées : inutile de solliciter DataStore à chaque frappe. */
    private val persistRequests = MutableStateFlow<StoredSession?>(null)

    init {
        viewModelScope.launch {
            // Anti-rebond : collectLatest annule le délai dès qu'une frappe suivante
            // arrive, donc seule la dernière valeur d'une rafale atteint le disque.
            // (Flow.debounce ferait la même chose mais reste une API @FlowPreview.)
            persistRequests.filterNotNull().collectLatest { request ->
                delay(PERSIST_DEBOUNCE_MS)
                sessionStore.save(request)
            }
        }

        // Les réglages d'affichage vivent hors de l'expression : on les suit en continu.
        viewModelScope.launch {
            sessionStore.settings.collect { stored ->
                settings = stored
                publish()
            }
        }

        val saved = savedInstanceSession()
        if (saved != null) {
            // Retour après mort du processus. SavedStateHandle est synchrone, donc
            // l'état est là immédiatement : pas de clignotement d'écran vide.
            restore(saved)
        } else {
            publish()
            viewModelScope.launch {
                val stored = sessionStore.session.first()
                // Ne pas écraser une saisie commencée pendant la lecture asynchrone.
                if (untouched) restore(stored)
            }
        }
    }

    fun onKey(action: KeyAction) {
        // Toute nouvelle frappe acquitte l'erreur affichée.
        error = null

        when (mode) {
            CalculationMode.CLASSIC -> onClassicKey(action)
            CalculationMode.RPN -> onRpnKey(action)
        }

        publish()
    }

    /**
     * Insère un nombre venu du presse-papiers.
     *
     * Le collage ne pose **aucun état nouveau** : il rejoue `appendDigit` / `appendSeparator`
     * caractère par caractère, exactement comme la restauration d'une session persistée. Un
     * tampon invalide ne peut donc pas naître d'un texte arbitraire.
     *
     * Un texte illisible est ignoré sans bruit — et c'est acceptable parce que l'interface
     * grise « Coller » dans ce cas : l'utilisateur ne peut pas déclencher un échec silencieux.
     */
    fun onPaste(text: String) {
        val pasted = ClipboardNumber.parseOrNull(text, buffer.notation) ?: return
        error = null

        when (mode) {
            CalculationMode.CLASSIC -> pasteIntoBuffer(pasted)
            CalculationMode.RPN -> pasteIntoRpn(pasted)
        }

        publish()
    }

    private fun pasteIntoBuffer(pasted: PastedNumber) {
        // Règle 5, comme pour un chiffre tapé : un nombre après un résultat repart de zéro.
        if (showingResult) reset()
        // Le signe passe par l'opérateur, seule forme qu'un négatif prend en infixe. Le
        // tampon l'acceptera ou le refusera selon ses propres règles — « 6−» collé après
        // « 2× » donne « 2×−6 », et un moins en tête d'expression est licite.
        if (pasted.negative) buffer = buffer.appendOperator(Operator.MINUS)
        buffer = replayInto(buffer, pasted.keys)
    }

    private fun pasteIntoRpn(pasted: PastedNumber) {
        pasted.keys.forEach { key ->
            rpn = if (key == ShadokFormatter.SEPARATOR) {
                rpn.appendSeparator()
            } else {
                rpn.appendDigit(key)
            }
        }
        // Après les chiffres : `negate` bascule le signe de la frappe en cours, alors qu'avant
        // eux il aurait changé celui du sommet de pile.
        if (pasted.negative) rpn = rpn.negate()
    }

    private fun replayInto(target: ExpressionBuffer, keys: String): ExpressionBuffer =
        keys.fold(target) { buffer, key ->
            if (key == ShadokFormatter.SEPARATOR) {
                buffer.appendSeparator()
            } else {
                buffer.appendDigit(key)
            }
        }

    fun onNotationChange(notation: NumberNotation) {
        // Les deux modes suivent : le sélecteur décimal/Shadok est un axe unique, il ne
        // doit pas laisser derrière lui un mode inactif écrit dans l'autre notation.
        buffer = buffer.withNotation(notation)
        rpn = rpn.withNotation(notation)
        publish()
    }

    /**
     * Bascule entre calculatrice classique et NPI.
     *
     * L'état de l'autre mode reste en place : revenir retrouve exactement l'expression, ou
     * la pile, qu'on y avait laissée.
     */
    fun onModeChange(target: CalculationMode) {
        if (target == mode) return
        mode = target
        error = null
        publish()
    }

    fun onSettingsChange(updated: DisplaySettings) {
        settings = updated
        publish()
        // Écriture immédiate : un réglage se change rarement, l'anti-rebond de la saisie
        // n'a pas de raison de s'appliquer ici.
        viewModelScope.launch { sessionStore.saveSettings(updated) }
    }

    // ------------------------------------------------------------- mode classique

    private fun onClassicKey(action: KeyAction) {
        when (action) {
            is KeyAction.Digit -> {
                // Règle 5 — un chiffre après un résultat repart de zéro, un opérateur
                // prolonge le résultat (`Logic.acceptInsert`, `Logic.java:158-160`).
                if (showingResult) reset()
                buffer = buffer.appendDigit(action.character)
            }

            KeyAction.Separator -> {
                if (showingResult) reset()
                buffer = buffer.appendSeparator()
            }

            is KeyAction.Op -> {
                continueFromResult()
                buffer = buffer.appendOperator(action.operator)
            }

            KeyAction.LeftParen -> {
                if (showingResult) reset()
                buffer = buffer.appendLeftParen()
            }

            KeyAction.RightParen -> {
                continueFromResult()
                buffer = buffer.appendRightParen()
            }

            KeyAction.Delete -> {
                continueFromResult()
                buffer = buffer.deleteLast()
            }

            KeyAction.Clear -> reset()

            KeyAction.Evaluate -> evaluate()

            // Touches propres à la NPI : le pavé classique ne les affiche pas.
            KeyAction.Enter, KeyAction.Swap, KeyAction.Drop, KeyAction.Negate -> Unit
        }
    }

    private fun reset() {
        buffer = buffer.clear()
        showingResult = false
        decimalApproximate = false
    }

    /** Le résultat affiché redevient un opérande ordinaire que l'on prolonge. */
    private fun continueFromResult() {
        showingResult = false
        decimalApproximate = false
    }

    private fun evaluate() {
        when (val result = Evaluator.evaluate(buffer)) {
            is EvalResult.Failure -> error = result.error

            is EvalResult.Success -> {
                val value = result.value
                // Le résultat devient le nouvel opérande, écrit en décimal : c'est la
                // seule notation qui rend exactement toute valeur issue d'une saisie
                // Shadok, puisque 4 = 2² et que 2 divise 10.
                buffer = ExpressionBuffer(
                    notation = buffer.notation,
                    atoms = listOf(Atom.Number(NumberNotation.DECIMAL, value.toDecimalString())),
                )
                showingResult = true
                // Un tiers, par exemple, n'a pas d'écriture décimale finie : le signaler
                // plutôt que de présenter un arrondi comme une valeur exacte.
                decimalApproximate = !value.hasFiniteDecimal
            }
        }
    }

    // ------------------------------------------------------------------ mode NPI

    private fun onRpnKey(action: KeyAction) {
        when (action) {
            is KeyAction.Digit -> rpn = rpn.appendDigit(action.character)

            KeyAction.Separator -> rpn = rpn.appendSeparator()

            // Un opérateur peut échouer — pile trop courte, division par zéro — et
            // l'échec laisse alors la session intacte : rien n'est perdu.
            is KeyAction.Op -> record(rpn.apply(action.operator))

            KeyAction.Swap -> record(rpn.swap())

            KeyAction.Enter -> rpn = rpn.enter()

            KeyAction.Drop -> rpn = rpn.dropTop()

            KeyAction.Negate -> rpn = rpn.negate()

            KeyAction.Delete -> rpn = rpn.deleteLast()

            KeyAction.Clear -> rpn = rpn.clear()

            // Touches propres à l'infixe : le pavé NPI ne les affiche pas. En postfixe,
            // il n'y a ni parenthèses ni « = » — l'ordre de frappe est l'ordre de calcul.
            KeyAction.Evaluate, KeyAction.LeftParen, KeyAction.RightParen -> Unit
        }
    }

    /**
     * Retient le résultat d'une opération de pile.
     *
     * En cas d'échec, `outcome.session` est l'état d'avant : l'affectation est donc sans
     * effet et seule l'erreur change. Rien à défaire.
     */
    private fun record(outcome: RpnOutcome) {
        rpn = outcome.session
        error = outcome.error
    }

    // ------------------------------------------------------------------ publication

    private fun publish() {
        val lines = renderLines()

        _uiState.value = CalculatorUiState(
            mode = mode,
            notation = buffer.notation,
            decimal = lines.decimal.text,
            glyphs = lines.glyphs.text,
            labels = lines.labels.text,
            base4 = lines.base4.text,
            shadokApproximate = lines.glyphs.approximate,
            decimalApproximate = when (mode) {
                // En infixe, une saisie décimale est rendue verbatim, donc toujours
                // exacte : seule l'évaluation peut produire un arrondi, et elle renseigne
                // le champ. En NPI, X peut être une valeur calculée qui traîne sur la pile.
                CalculationMode.CLASSIC -> decimalApproximate

                CalculationMode.RPN -> lines.decimal.approximate
            },
            stack = stackLevels(),
            // Hors NPI la notion n'existe pas : une expression infixe n'est jamais « en
            // attente d'empilement ».
            entering = mode == CalculationMode.RPN && rpn.entering,
            error = error,
            showingResult = showingResult,
            settings = settings,
        )

        val session = storedSession()
        saveInstanceState(session)
        persistRequests.value = session
    }

    /** Les quatre projections de la ligne principale, selon le mode affiché. */
    private fun renderLines(): Lines = when (mode) {
        CalculationMode.CLASSIC -> Lines(
            decimal = buffer.render(ExpressionDisplay.DECIMAL),
            glyphs = buffer.render(ExpressionDisplay.SHADOK_GLYPHS),
            labels = buffer.render(ExpressionDisplay.SHADOK_LABELS),
            base4 = buffer.render(ExpressionDisplay.SHADOK_BASE4),
        )

        CalculationMode.RPN -> Lines(
            decimal = rpn.renderX(ExpressionDisplay.DECIMAL),
            glyphs = rpn.renderX(ExpressionDisplay.SHADOK_GLYPHS),
            labels = rpn.renderX(ExpressionDisplay.SHADOK_LABELS),
            base4 = rpn.renderX(ExpressionDisplay.SHADOK_BASE4),
        )
    }

    /** Les niveaux sous X. Vide hors NPI : la calculatrice classique n'a pas de pile. */
    private fun stackLevels(): List<StackLevel> {
        if (mode != CalculationMode.RPN) return emptyList()
        val decimal = rpn.renderBelowX(ExpressionDisplay.DECIMAL)
        val glyphs = rpn.renderBelowX(ExpressionDisplay.SHADOK_GLYPHS)
        val labels = rpn.renderBelowX(ExpressionDisplay.SHADOK_LABELS)
        // Comme la ligne de X : jamais affichée, mais un appui long sur un niveau enfoui
        // doit pouvoir le copier dans les quatre écritures.
        val base4 = rpn.renderBelowX(ExpressionDisplay.SHADOK_BASE4)
        return decimal.indices.map { level ->
            StackLevel(
                glyphs = glyphs[level].text,
                labels = labels[level].text,
                decimal = decimal[level].text,
                base4 = base4[level].text,
                shadokApproximate = glyphs[level].approximate,
                decimalApproximate = decimal[level].approximate,
            )
        }
    }

    // ------------------------------------------------------------------ persistance

    /** Rien n'a encore été saisi, dans aucun des deux modes. */
    private val untouched: Boolean
        get() = buffer.isEmpty && rpn.stack.isEmpty && !rpn.entering

    private fun storedSession(): StoredSession = StoredSession(
        keys = buffer.replayKeys(),
        notation = buffer.notation,
        mode = mode,
        rpn = StoredRpn(
            stack = rpn.stack.keys(),
            entry = rpn.entryKeys(),
            entryNegative = rpn.entryNegative,
        ),
    )

    private fun restore(session: StoredSession) {
        buffer = ExpressionBuffer.replay(session.keys, session.notation)
        rpn = RpnSession.restore(
            stackKeys = session.rpn.stack,
            entryKeys = session.rpn.entry,
            entryNegative = session.rpn.entryNegative,
            notation = session.notation,
        )
        mode = session.mode
        showingResult = false
        decimalApproximate = false
        error = null
        publish()
    }

    private fun saveInstanceState(session: StoredSession) {
        savedStateHandle[STATE_KEYS] = session.keys
        savedStateHandle[STATE_NOTATION] = session.notation.name
        savedStateHandle[STATE_MODE] = session.mode.name
        savedStateHandle[STATE_RPN_STACK] = session.rpn.stack
        savedStateHandle[STATE_RPN_ENTRY] = session.rpn.entry
        savedStateHandle[STATE_RPN_NEGATIVE] = session.rpn.entryNegative
    }

    /** L'état sauvé par le système, ou `null` s'il s'agit d'un lancement à froid. */
    private fun savedInstanceSession(): StoredSession? {
        val keys = savedStateHandle.get<String>(STATE_KEYS) ?: return null
        return StoredSession(
            keys = keys,
            notation = notationNamed(savedStateHandle[STATE_NOTATION]),
            mode = modeNamed(savedStateHandle[STATE_MODE]),
            rpn = StoredRpn(
                stack = savedStateHandle.get<String>(STATE_RPN_STACK).orEmpty(),
                entry = savedStateHandle.get<String>(STATE_RPN_ENTRY).orEmpty(),
                entryNegative = savedStateHandle.get<Boolean>(STATE_RPN_NEGATIVE) == true,
            ),
        )
    }

    private fun notationNamed(name: String?): NumberNotation =
        NumberNotation.entries.firstOrNull { it.name == name } ?: NumberNotation.DECIMAL

    private fun modeNamed(name: String?): CalculationMode =
        CalculationMode.entries.firstOrNull { it.name == name } ?: CalculationMode.CLASSIC

    /** Les trois écritures de la ligne principale, rendues d'un seul coup. */
    private data class Lines(
        val decimal: Rendered,
        val glyphs: Rendered,
        val labels: Rendered,
        /** Jamais affichée : elle n'existe que pour être copiée. */
        val base4: Rendered,
    )

    companion object {
        private const val STATE_KEYS = "expression-keys"
        private const val STATE_NOTATION = "expression-notation"
        private const val STATE_MODE = "calculation-mode"
        private const val STATE_RPN_STACK = "rpn-stack"
        private const val STATE_RPN_ENTRY = "rpn-entry"
        private const val STATE_RPN_NEGATIVE = "rpn-entry-negative"
        private const val PERSIST_DEBOUNCE_MS = 300L

        /**
         * Injection par constructeur, sans framework de DI : quatre modules et un seul
         * ViewModel ne justifient pas Hilt.
         */
        fun factory(context: Context): ViewModelProvider.Factory {
            val applicationContext = context.applicationContext
            return viewModelFactory {
                initializer {
                    CalculatorViewModel(
                        sessionStore = CalculatorPreferences(applicationContext),
                        savedStateHandle = createSavedStateHandle(),
                    )
                }
            }
        }
    }
}
