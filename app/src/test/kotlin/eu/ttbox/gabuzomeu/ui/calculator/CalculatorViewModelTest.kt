package eu.ttbox.gabuzomeu.ui.calculator

import androidx.lifecycle.SavedStateHandle
import eu.ttbox.gabuzomeu.core.eval.EvalError
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import eu.ttbox.gabuzomeu.core.eval.Operator
import eu.ttbox.gabuzomeu.data.DisplaySettings
import eu.ttbox.gabuzomeu.data.SessionStore
import eu.ttbox.gabuzomeu.data.StoredSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests du ViewModel sur la JVM — ni émulateur, ni Robolectric.
 *
 * C'est possible parce que le ViewModel dépend de l'interface [SessionStore] et non d'un
 * `Context`. Dans le projet d'origine, la moindre vérification de ce comportement
 * exigeait un appareil : `Calculator` héritait de `FragmentActivity` et la persistance
 * appelait directement `Context.openFileInput`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CalculatorViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var store: FakeSessionStore

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        store = FakeSessionStore()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        CalculatorViewModel(sessionStore = store, savedStateHandle = savedState)

    // ------------------------------------------------------- projections simultanées

    @Test
    fun `les trois affichages sont produits d'un seul coup`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('6'))

        val state = model.uiState.value
        assertEquals("6", state.decimal)
        assertEquals("_⅃", state.glyphs)
        assertEquals("BuZo", state.labels)
        assertFalse(state.shadokApproximate)
    }

    @Test
    fun `une decimale non representable en base 4 est signalee`() = runTest {
        val model = viewModel()

        "0.1".forEach { key ->
            if (key == '.') model.onKey(KeyAction.Separator) else model.onKey(KeyAction.Digit(key))
        }

        assertTrue(model.uiState.value.shadokApproximate)
    }

    // ---------------------------------------------------------------- évaluation

    @Test
    fun `evaluer remplace l'expression par son resultat`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('6'))
        model.onKey(KeyAction.Op(Operator.TIMES))
        model.onKey(KeyAction.Digit('7'))
        model.onKey(KeyAction.Evaluate)

        val state = model.uiState.value
        assertEquals("42", state.decimal)
        // 42 en base 4 = 222, soit ZoZoZo.
        assertEquals("ZoZoZo", state.labels)
        assertTrue(state.showingResult)
    }

    @Test
    fun `un resultat sans ecriture decimale finie est signale`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('1'))
        model.onKey(KeyAction.Op(Operator.DIVIDE))
        model.onKey(KeyAction.Digit('3'))
        model.onKey(KeyAction.Evaluate)

        assertTrue(model.uiState.value.decimalApproximate, "1/3 n'a pas d'ecriture decimale finie")
    }

    @Test
    fun `la division par zero remonte une erreur et laisse l'expression intacte`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('5'))
        model.onKey(KeyAction.Op(Operator.DIVIDE))
        model.onKey(KeyAction.Digit('0'))
        model.onKey(KeyAction.Evaluate)

        assertEquals(EvalError.DIVISION_BY_ZERO, model.uiState.value.error)
        assertEquals("5÷0", model.uiState.value.decimal)
    }

    @Test
    fun `toute nouvelle frappe acquitte l'erreur`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('5'))
        model.onKey(KeyAction.Op(Operator.DIVIDE))
        model.onKey(KeyAction.Digit('0'))
        model.onKey(KeyAction.Evaluate)
        assertEquals(EvalError.DIVISION_BY_ZERO, model.uiState.value.error)

        model.onKey(KeyAction.Delete)
        assertNull(model.uiState.value.error)
    }

    // ------------------------------------------------------------------- règle 5

    @Test
    fun `regle 5 - un chiffre apres un resultat repart de zero`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('2'))
        model.onKey(KeyAction.Op(Operator.PLUS))
        model.onKey(KeyAction.Digit('3'))
        model.onKey(KeyAction.Evaluate)
        assertEquals("5", model.uiState.value.decimal)

        model.onKey(KeyAction.Digit('7'))
        assertEquals("7", model.uiState.value.decimal)
        assertFalse(model.uiState.value.showingResult)
    }

    @Test
    fun `regle 5 - un operateur apres un resultat le prolonge`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('2'))
        model.onKey(KeyAction.Op(Operator.PLUS))
        model.onKey(KeyAction.Digit('3'))
        model.onKey(KeyAction.Evaluate)

        model.onKey(KeyAction.Op(Operator.TIMES))
        model.onKey(KeyAction.Digit('2'))
        model.onKey(KeyAction.Evaluate)

        assertEquals("10", model.uiState.value.decimal)
    }

    // --------------------------------------------------------- changement de mode

    @Test
    fun `changer de mode convertit l'expression et bascule le pave`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('6'))
        model.onNotationChange(NumberNotation.SHADOK)

        val state = model.uiState.value
        assertEquals(NumberNotation.SHADOK, state.notation)
        assertEquals("6", state.decimal)
        assertEquals("_⅃", state.glyphs)
    }

    @Test
    fun `en mode Shadok seuls les glyphes sont acceptes`() = runTest {
        val model = viewModel()
        model.onNotationChange(NumberNotation.SHADOK)

        model.onKey(KeyAction.Digit('7'))
        assertTrue(model.uiState.value.isEmpty, "un 7 decimal n'a pas de sens en base 4")

        model.onKey(KeyAction.Digit('◿'))
        assertEquals("3", model.uiState.value.decimal)
    }

    // ------------------------------------------------------------- persistance

    @Test
    fun `l'expression est persistee apres l'anti-rebond`() = runTest {
        val model = viewModel()

        model.onKey(KeyAction.Digit('1'))
        model.onKey(KeyAction.Digit('2'))
        advanceUntilIdle()

        assertEquals("12", store.saved.value.keys)
        assertEquals(NumberNotation.DECIMAL, store.saved.value.notation)
    }

    @Test
    fun `l'anti-rebond ne garde que la derniere frappe d'une rafale`() = runTest {
        val model = viewModel()

        repeat(6) { model.onKey(KeyAction.Digit('1')) }
        advanceUntilIdle()

        assertEquals("111111", store.saved.value.keys)
        // Une seule ecriture disque pour toute la rafale.
        assertEquals(1, store.saveCount)
    }

    @Test
    fun `SavedStateHandle restaure l'etat apres mort du processus`() = runTest {
        val savedState = SavedStateHandle()
        val first = viewModel(savedState)
        first.onKey(KeyAction.Digit('4'))
        first.onKey(KeyAction.Op(Operator.PLUS))
        first.onKey(KeyAction.Digit('2'))

        // Nouveau ViewModel, meme SavedStateHandle : ce que fait le systeme au retour.
        val restored = viewModel(savedState)

        assertEquals("4+2", restored.uiState.value.decimal)
    }

    @Test
    fun `le magasin restaure la derniere session au lancement a froid`() = runTest {
        store.saved.value = StoredSession(keys = "⅃⅃", notation = NumberNotation.SHADOK)

        val model = viewModel()
        advanceUntilIdle()

        assertEquals(NumberNotation.SHADOK, model.uiState.value.notation)
        // ⅃⅃ = 22 en base 4 = 10 en decimal.
        assertEquals("10", model.uiState.value.decimal)
    }

    @Test
    fun `une saisie commencee n'est pas ecrasee par la restauration asynchrone`() = runTest {
        store.saved.value = StoredSession(keys = "99", notation = NumberNotation.DECIMAL)

        val model = viewModel()
        // L'utilisateur tape avant que la lecture disque ne se termine.
        model.onKey(KeyAction.Digit('7'))
        advanceUntilIdle()

        assertEquals("7", model.uiState.value.decimal)
    }

    // ------------------------------------------------------- réglages d'affichage

    @Test
    fun `les reglages par defaut affichent les trois lignes`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        val settings = model.uiState.value.settings
        assertTrue(settings.showShadokLabels)
        assertTrue(settings.showDecimal)
    }

    @Test
    fun `masquer les noms Shadok est publie et persiste`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        model.onSettingsChange(DisplaySettings(showShadokLabels = false))
        advanceUntilIdle()

        assertFalse(model.uiState.value.settings.showShadokLabels)
        assertTrue(model.uiState.value.settings.showDecimal)
        assertFalse(store.savedSettings.value.showShadokLabels)
    }

    @Test
    fun `masquer le decimal est publie et persiste`() = runTest {
        val model = viewModel()
        advanceUntilIdle()

        model.onSettingsChange(DisplaySettings(showDecimal = false))
        advanceUntilIdle()

        assertFalse(model.uiState.value.settings.showDecimal)
        assertFalse(store.savedSettings.value.showDecimal)
    }

    @Test
    fun `les reglages stockes sont restaures au lancement`() = runTest {
        store.savedSettings.value = DisplaySettings(showShadokLabels = false, showDecimal = false)

        val model = viewModel()
        advanceUntilIdle()

        assertFalse(model.uiState.value.settings.showShadokLabels)
        assertFalse(model.uiState.value.settings.showDecimal)
    }

    private class FakeSessionStore : SessionStore {
        val saved = MutableStateFlow(StoredSession())
        val savedSettings = MutableStateFlow(DisplaySettings())
        var saveCount: Int = 0
            private set

        override val session: Flow<StoredSession> = saved
        override val settings: Flow<DisplaySettings> = savedSettings

        override suspend fun save(keys: String, notation: NumberNotation) {
            saveCount++
            saved.value = StoredSession(keys, notation)
        }

        override suspend fun saveSettings(settings: DisplaySettings) {
            savedSettings.value = settings
        }
    }
}
