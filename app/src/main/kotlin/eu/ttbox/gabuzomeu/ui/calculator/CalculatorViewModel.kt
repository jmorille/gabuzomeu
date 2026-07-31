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
import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.eval.EvalResult
import eu.ttbox.gabuzomeu.core.eval.Evaluator
import eu.ttbox.gabuzomeu.core.eval.ExpressionBuffer
import eu.ttbox.gabuzomeu.core.eval.ExpressionDisplay
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.core.eval.Operator
import eu.ttbox.gabuzomeu.data.CalculatorPreferences
import eu.ttbox.gabuzomeu.data.SessionStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Ce qu'une touche du pavé demande de faire. */
sealed interface KeyAction {
    data class Digit(val character: Char) : KeyAction
    data class Op(val operator: Operator) : KeyAction
    data object Separator : KeyAction
    data object LeftParen : KeyAction
    data object RightParen : KeyAction
    data object Delete : KeyAction
    data object Clear : KeyAction
    data object Evaluate : KeyAction
}

class CalculatorViewModel(
    private val sessionStore: SessionStore,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private var buffer = ExpressionBuffer()
    private var showingResult = false
    private var error: EvalError? = null
    private var decimalApproximate = false

    private val _uiState = MutableStateFlow(CalculatorUiState())
    val uiState: StateFlow<CalculatorUiState> = _uiState.asStateFlow()

    /** Écritures disque regroupées : inutile de solliciter DataStore à chaque frappe. */
    private val persistRequests = MutableStateFlow<StoredKeys?>(null)

    init {
        viewModelScope.launch {
            // Anti-rebond : collectLatest annule le délai dès qu'une frappe suivante
            // arrive, donc seule la dernière valeur d'une rafale atteint le disque.
            // (Flow.debounce ferait la même chose mais reste une API @FlowPreview.)
            persistRequests.filterNotNull().collectLatest { request ->
                delay(PERSIST_DEBOUNCE_MS)
                sessionStore.save(request.keys, request.notation)
            }
        }

        val savedKeys = savedStateHandle.get<String>(STATE_KEYS)
        if (savedKeys != null) {
            // Retour après mort du processus. SavedStateHandle est synchrone, donc
            // l'état est là immédiatement : pas de clignotement d'écran vide.
            restore(savedKeys, notationNamed(savedStateHandle[STATE_NOTATION]))
        } else {
            publish()
            viewModelScope.launch {
                val stored = sessionStore.session.first()
                // Ne pas écraser une saisie commencée pendant la lecture asynchrone.
                if (buffer.isEmpty) restore(stored.keys, stored.notation)
            }
        }
    }

    fun onKey(action: KeyAction) {
        // Toute nouvelle frappe acquitte l'erreur affichée.
        error = null

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
        }

        publish()
    }

    fun onNotationChange(notation: NumberNotation) {
        buffer = buffer.withNotation(notation)
        publish()
    }

    // ------------------------------------------------------------------ interne

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

    private fun publish() {
        val decimal = buffer.render(ExpressionDisplay.DECIMAL)
        val glyphs = buffer.render(ExpressionDisplay.SHADOK_GLYPHS)
        val labels = buffer.render(ExpressionDisplay.SHADOK_LABELS)

        _uiState.value = CalculatorUiState(
            notation = buffer.notation,
            decimal = decimal.text,
            glyphs = glyphs.text,
            labels = labels.text,
            shadokApproximate = glyphs.approximate,
            decimalApproximate = decimalApproximate,
            error = error,
            showingResult = showingResult,
        )

        val keys = buffer.replayKeys()
        savedStateHandle[STATE_KEYS] = keys
        savedStateHandle[STATE_NOTATION] = buffer.notation.name
        persistRequests.value = StoredKeys(keys, buffer.notation)
    }

    private fun restore(keys: String, notation: NumberNotation) {
        buffer = ExpressionBuffer.replay(keys, notation)
        showingResult = false
        decimalApproximate = false
        error = null
        publish()
    }

    private fun notationNamed(name: String?): NumberNotation =
        NumberNotation.entries.firstOrNull { it.name == name } ?: NumberNotation.DECIMAL

    private data class StoredKeys(val keys: String, val notation: NumberNotation)

    companion object {
        private const val STATE_KEYS = "expression-keys"
        private const val STATE_NOTATION = "expression-notation"
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
