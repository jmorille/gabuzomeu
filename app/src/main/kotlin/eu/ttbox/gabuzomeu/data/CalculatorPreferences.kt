package eu.ttbox.gabuzomeu.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import eu.ttbox.gabuzomeu.core.eval.CalculationMode
import eu.ttbox.gabuzomeu.core.eval.NumberNotation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.calculatorDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "calculator",
)

/**
 * Implémentation de [SessionStore] sur DataStore Preferences.
 *
 * Remplace `Persist.java` du projet d'origine : un format binaire maison écrit avec
 * `DataOutputStream` dans `calculator.data`, dont les contrôles de version étaient
 * inversés (`Persist.java:55-59` — la branche « version trop récente » était
 * inatteignable) et dont toutes les exceptions étaient avalées par un `log()` lui-même
 * désactivé (`Calculator.LOG_ENABLED = false`).
 *
 * L'historique des calculs n'est pas conservé : la fonctionnalité était déjà morte dans
 * le projet d'origine, `HistoryAdapter` n'étant branché sur aucune `ListView`.
 */
class CalculatorPreferences(context: Context) : SessionStore {

    private val dataStore = context.calculatorDataStore

    override val session: Flow<StoredSession> = dataStore.data.map { preferences ->
        StoredSession(
            keys = preferences[KEY_EXPRESSION].orEmpty(),
            notation = preferences[KEY_NOTATION]
                ?.let { stored -> NumberNotation.entries.firstOrNull { it.name == stored } }
                ?: NumberNotation.DECIMAL,
            mode = preferences[KEY_MODE]
                ?.let { stored -> CalculationMode.entries.firstOrNull { it.name == stored } }
                ?: CalculationMode.CLASSIC,
            rpn = StoredRpn(
                stack = preferences[KEY_RPN_STACK].orEmpty(),
                entry = preferences[KEY_RPN_ENTRY].orEmpty(),
                entryNegative = preferences[KEY_RPN_NEGATIVE] == true,
            ),
        )
    }

    override val settings: Flow<DisplaySettings> = dataStore.data.map { preferences ->
        val defaults = DisplaySettings()
        DisplaySettings(
            showShadokLabels = preferences[KEY_SHOW_LABELS] ?: defaults.showShadokLabels,
            showDecimal = preferences[KEY_SHOW_DECIMAL] ?: defaults.showDecimal,
        )
    }

    override suspend fun save(session: StoredSession) {
        dataStore.edit { preferences ->
            preferences[KEY_EXPRESSION] = session.keys
            preferences[KEY_NOTATION] = session.notation.name
            preferences[KEY_MODE] = session.mode.name
            preferences[KEY_RPN_STACK] = session.rpn.stack
            preferences[KEY_RPN_ENTRY] = session.rpn.entry
            preferences[KEY_RPN_NEGATIVE] = session.rpn.entryNegative
        }
    }

    override suspend fun saveSettings(settings: DisplaySettings) {
        dataStore.edit { preferences ->
            preferences[KEY_SHOW_LABELS] = settings.showShadokLabels
            preferences[KEY_SHOW_DECIMAL] = settings.showDecimal
        }
    }

    private companion object {
        val KEY_EXPRESSION = stringPreferencesKey("expression")
        val KEY_NOTATION = stringPreferencesKey("notation")
        val KEY_MODE = stringPreferencesKey("calculation-mode")
        val KEY_RPN_STACK = stringPreferencesKey("rpn-stack")
        val KEY_RPN_ENTRY = stringPreferencesKey("rpn-entry")
        val KEY_RPN_NEGATIVE = booleanPreferencesKey("rpn-entry-negative")
        val KEY_SHOW_LABELS = booleanPreferencesKey("show-shadok-labels")
        val KEY_SHOW_DECIMAL = booleanPreferencesKey("show-decimal")
    }
}
